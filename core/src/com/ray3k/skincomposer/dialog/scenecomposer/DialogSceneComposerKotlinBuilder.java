package com.ray3k.skincomposer.dialog.scenecomposer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.ray3k.skincomposer.data.ColorData;
import com.ray3k.skincomposer.data.DrawableData;
import com.ray3k.skincomposer.utils.Utils;
import com.squareup.kotlinpoet.*;

import static com.ray3k.skincomposer.dialog.scenecomposer.DialogSceneComposerModel.*;

public class DialogSceneComposerKotlinBuilder {
    private final static ObjectMap<Class, ClassName> classNames = new ObjectMap<>();
    private static ClassName nodeClassName;

    private static ClassName getSimpleClassName(Class clazz) {
        ClassName className = classNames.get(clazz);
        if (className == null) className = new ClassName("", clazz.getSimpleName());
        return className;
    }

    public static String generateKotlinFile() {
        nodeClassName = new ClassName(rootActor.packageString + "." + rootActor.classString, "BasicNode");

        TypeSpec.Builder typeSpec = TypeSpec.classBuilder(rootActor.classString)
            .superclass(ApplicationAdapter.class)
            .addProperty(
                PropertySpec.builder("skin", Skin.class)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("%T(%T.files.internal(%S))", getSimpleClassName(Skin.class), getSimpleClassName(Gdx.class), rootActor.skinPath)
                    .build())
            .addProperty(
                PropertySpec.builder("stage", Stage.class)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("%T(%T())", getSimpleClassName(Stage.class), getSimpleClassName(ScreenViewport.class))
                    .build()
            )
            .addFunction(createMethod((clazz) -> getSimpleClassName(clazz)))
            .addFunction(renderMethod())
            .addFunction(resizeMethod())
            .addFunction(disposeMethod());

        if (rootActor.hasChildOfTypeRecursive(SimTree.class)) {
            typeSpec.addType(basicNodeType());
        }

        FileSpec kotlinFile = FileSpec.builder(rootActor.packageString, rootActor.classString.toLowerCase())
            .addType(
                typeSpec.build()
            )
            .build();

        return kotlinFile.toString();
    }

    public static String generateClipBoard() {
        nodeClassName = new ClassName("", "BasicNode");
        return createMethod((clazz) -> getSimpleClassName(clazz)).getBody().toString();
    }

    private static FunSpec createMethod(ClassNameGetter classNameGetter) {
        return FunSpec.builder("create")
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("val stage = %T(%T())", classNameGetter.get(Stage.class), classNameGetter.get(ScreenViewport.class))
            .addStatement("val skin = %T(%T.files.internal(%S))", classNameGetter.get(Skin.class), classNameGetter.get(Gdx.class), rootActor.skinPath)
            .addStatement("%T.input.inputProcessor = stage", classNameGetter.get(Gdx.class))
            .addCode(createWidget(rootActor, new Array<>(), new ObjectSet<>(), classNameGetter).codeBlock)
            .build();
    }

    private static WidgetNamePair createWidget(SimActor actor, Array<String> variables, ObjectSet<String> usedVariables, ClassNameGetter classNameGetter) {
        if (actor == null) return null;
        else if (actor instanceof SimRootGroup) {
            var simRootGroup = (SimRootGroup) actor;
            var builder = CodeBlock.builder();

            for (var child : simRootGroup.children) {
                var pair = createWidget(child, variables, usedVariables, classNameGetter);
                builder.add("\n");
                builder.add(pair.codeBlock);
                builder.addStatement("stage.addActor(%L)", pair.name);
                variables.removeValue(pair.name, false);
                usedVariables.add(pair.name);
            }

            return new WidgetNamePair(builder.build(), null);
        } else if (actor instanceof SimTable) {
            var table = (SimTable) actor;
            var builder = CodeBlock.builder();
            var variableName = createVariableName("table", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ", classNameGetter.get(Table.class));
            builder.addStatement("%L = %T()", variableName, classNameGetter.get(Table.class));

            if (table.name != null) addSetNameStatement(builder, variableName, table.name);

            if (!table.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (table.touchable != Touchable.childrenOnly) {
                addSetTouchableStatement(builder, variableName, classNameGetter, table.touchable);
            }

            if (table.background != null) addSetBackgroundStatement(builder, variableName, table.background);
            if (table.color != null) addSetColorStatement(builder, variableName, table.color);

            if (table.paddingEnabled) {
                addPadStatements(builder, variableName, table.padLeft, table.padRight, table.padTop, table.padBottom);
            }

            if (table.alignment != Align.center) {
                addAlignStatement(builder, variableName, classNameGetter, table.alignment);
            }

            if (table.fillParent) addFSetFillParentStatement(builder, variableName);

            int row = 0;
            for (var cell : table.getChildren()) {
                builder.add("\n");
                if (cell.row > row) {
                    row = cell.row;
                    builder.addStatement("%L.row()", variableName);
                }

                WidgetNamePair pair = createWidget(cell.child, variables, usedVariables, classNameGetter);
                if (pair != null) {
                    builder.add(pair.codeBlock);
                }

                builder.add("%L.add(%L)", variableName, pair == null ? "" : pair.name);

                addChildPaddingStatements(builder, cell.padLeft, cell.padRight, cell.padTop, cell.padBottom);
                addChildSpaceStatements(builder, cell.spaceLeft, cell.spaceRight, cell.spaceTop, cell.spaceBottom);
                addChildGrowExpandAndFillStatements(builder, cell.growX, cell.growY, cell.expandX, cell.expandY, cell.fillX, cell.fillY);
                addChildAlignStatement(builder, classNameGetter, cell.alignment);
                addChildWidthHeightAndSizeStatements(builder, cell.minWidth, cell.minHeight, cell.maxWidth, cell.maxHeight, cell.preferredWidth, cell.preferredHeight);
                addChildUniformStatements(builder, cell.uniformX, cell.uniformY);

                if (cell.colSpan > 1) addChildColSpanStatement(builder, cell.colSpan);

                builder.addStatement("");
                if (pair != null) {
                    variables.removeValue(pair.name, false);
                    usedVariables.add(pair.name);
                }
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimButton) {
            var button = (SimButton) actor;
            if (button.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("button", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(skin%L)", variableName, classNameGetter.get(Button.class),
                button.style.name.equals("default") ? "" : ", \"" + button.style.name + "\"");

            if (button.name != null) addSetNameStatement(builder, variableName, button.name);

            if (!button.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (button.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, button.touchable);
            }

            if (button.checked) addSetIsCheckedStatement(builder, variableName, true);
            if (button.disabled) addSetIsDisabledStatement(builder, variableName, true);
            if (button.color != null) addSetColorStatement(builder, variableName, button.color);

            if (!Utils.isEqual(0, button.padLeft, button.padRight, button.padTop, button.padBottom)) {
                addPadStatements(builder, variableName, button.padLeft, button.padRight, button.padTop, button.padBottom);
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimCheckBox) {
            var checkBox = (SimCheckBox) actor;
            if (checkBox.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("checkBox", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(%S, skin%L)", variableName, classNameGetter.get(CheckBox.class), checkBox.text,
                checkBox.style.name.equals("default") ? "" : ", \"" + checkBox.style.name + "\"");

            if (checkBox.name != null) addSetNameStatement(builder, variableName, checkBox.name);

            if (!checkBox.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (checkBox.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, checkBox.touchable);
            }

            if (checkBox.checked) addSetIsCheckedStatement(builder, variableName, true);
            if (checkBox.disabled) addSetIsDisabledStatement(builder, variableName, true);
            if (checkBox.color != null) addSetColorStatement(builder, variableName, checkBox.color);

            if (!Utils.isEqual(0, checkBox.padLeft, checkBox.padRight, checkBox.padTop, checkBox.padBottom)) {
                addPadStatements(builder, variableName, checkBox.padLeft, checkBox.padRight, checkBox.padTop, checkBox.padBottom);
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimImage) {
            var image = (SimImage) actor;
            if (image.drawable == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("image", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(skin, %S)", variableName, classNameGetter.get(Image.class), image.drawable.name);
            if (image.name != null) addSetNameStatement(builder, variableName, image.name);

            if (!image.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (image.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, image.touchable);
            }

            if (image.scaling != null && !image.scaling.equals("stretch"))
                addSetScalingStatement(builder, variableName, classNameGetter, image.scaling);

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimImageButton) {
            var imageButton = (SimImageButton) actor;
            if (imageButton.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("imageButton", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(skin%L)", variableName, classNameGetter.get(ImageButton.class),
                imageButton.style.name.equals("default") ? "" : ", \"" + imageButton.style.name + "\"");

            if (imageButton.name != null) addSetNameStatement(builder, variableName, imageButton.name);

            if (!imageButton.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (imageButton.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, imageButton.touchable);
            }

            if (imageButton.checked) addSetIsCheckedStatement(builder, variableName, true);
            if (imageButton.disabled) addSetIsDisabledStatement(builder, variableName, true);
            if (imageButton.color != null) addSetColorStatement(builder, variableName, imageButton.color);

            if (!Utils.isEqual(0, imageButton.padLeft, imageButton.padRight, imageButton.padTop, imageButton.padBottom)) {
                addPadStatements(builder, variableName, imageButton.padLeft, imageButton.padRight, imageButton.padTop, imageButton.padBottom);
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimImageTextButton) {
            var imageTextButton = (SimImageTextButton) actor;
            if (imageTextButton.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("imageTextButton", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(%S, skin%L)", variableName, classNameGetter.get(ImageTextButton.class), convertEscapedCharacters(imageTextButton.text),
                imageTextButton.style.name.equals("default") ? "" : ", \"" + imageTextButton.style.name + "\"");

            if (imageTextButton.name != null)
                addSetNameStatement(builder, variableName, imageTextButton.name);

            if (!imageTextButton.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (imageTextButton.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, imageTextButton.touchable);
            }

            if (imageTextButton.checked) addSetIsCheckedStatement(builder, variableName, true);
            if (imageTextButton.disabled) addSetIsDisabledStatement(builder, variableName, true);
            if (imageTextButton.color != null) addSetColorStatement(builder, variableName, imageTextButton.color);

            if (!Utils.isEqual(0, imageTextButton.padLeft, imageTextButton.padRight, imageTextButton.padTop, imageTextButton.padBottom)) {
                addPadStatements(builder, variableName, imageTextButton.padLeft, imageTextButton.padRight, imageTextButton.padTop, imageTextButton.padBottom);
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimLabel) {
            var label = (SimLabel) actor;
            if (label.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("label", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(%S, skin%L)", variableName, classNameGetter.get(Label.class), convertEscapedCharacters(label.text),
                label.style.name.equals("default") ? "" : ", \"" + label.style.name + "\"");

            if (label.name != null) addSetNameStatement(builder, variableName, label.name);

            if (!label.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (label.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, label.touchable);
            }

            if (label.textAlignment != Align.left) {
                addSetAlignmentStatement(builder, variableName, classNameGetter, label.textAlignment);
            }

            if (label.ellipsis && label.ellipsisString != null) {
                if (label.ellipsisString.equals("...")) addSetEllipsisStatement(builder, variableName, true);
                else addSetEllipsisStatement(builder, variableName, label.ellipsisString);
            }

            if (label.wrap) addSetWrapStatement(builder, variableName, true);
            if (label.color != null) addSetColorStatement(builder, variableName, label.color);

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimList) {
            var list = (SimList) actor;
            if (list.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("list", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T<String>(skin%L)", variableName, classNameGetter.get(List.class),
                list.style.name.equals("default") ? "" : ", \"" + list.style.name + "\"");

            if (list.name != null) addSetNameStatement(builder, variableName, list.name);

            if (!list.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (list.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, list.touchable);
            }

            if (list.list.size > 0) {
                builder.add("%L.setItems(", variableName);
                boolean addComma = false;
                for (var item : list.list) {
                    builder.add((addComma ? ", " : "") + "%S", item);
                    addComma = true;
                }
                builder.addStatement(")");
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimProgressBar) {
            var progressBar = (SimProgressBar) actor;
            if (progressBar.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("progressBar", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(%Lf, %Lf, %Lf, %L, skin%L)", variableName, classNameGetter.get(ProgressBar.class),
                progressBar.minimum, progressBar.maximum, progressBar.increment, progressBar.vertical,
                progressBar.style.name.equals("default-horizontal") || progressBar.style.name.equals("default-vertical") ? "" : ", \"" + progressBar.style.name + "\"");

            if (progressBar.name != null) addSetNameStatement(builder, variableName, progressBar.name);

            if (!progressBar.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (progressBar.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, progressBar.touchable);
            }

            if (!MathUtils.isZero(progressBar.value)) addSetValueStatement(builder, variableName, progressBar.value);
            if (!MathUtils.isZero(progressBar.animationDuration))
                addSetAnimationDurationStatement(builder, variableName, progressBar.animationDuration);

            if (progressBar.animateInterpolation != null && progressBar.animateInterpolation != Interpol.LINEAR)
                addSetAnimateInterpolationStatement(builder, variableName, classNameGetter, progressBar.animateInterpolation);

            if (!progressBar.round) addSetRoundStatement(builder, variableName, false);
            if (progressBar.visualInterpolation != null && progressBar.visualInterpolation != Interpol.LINEAR)
                addSetVisualInterpolationStatement(builder, variableName, classNameGetter, progressBar.visualInterpolation);

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimSelectBox) {
            var selectBox = (SimSelectBox) actor;
            if (selectBox.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("selectBox", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T<%T>(skin%L)", variableName, classNameGetter.get(SelectBox.class),
                classNameGetter.get(String.class), selectBox.style.name.equals("default") ? "" : ", \"" + selectBox.style.name + "\"");

            if (selectBox.name != null) addSetNameStatement(builder, variableName, selectBox.name);

            if (!selectBox.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (selectBox.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, selectBox.touchable);
            }

            if (selectBox.disabled) addSetIsDisabledStatement(builder, variableName, selectBox.disabled);
            if (selectBox.maxListCount != 0) addSetMaxListCountStatement(builder, variableName, selectBox.maxListCount);

            if (selectBox.list.size > 0) {
                builder.add("%L.setItems(", variableName);
                boolean addComma = false;
                for (var item : selectBox.list) {
                    builder.add((addComma ? ", " : "") + "%S", item);
                    addComma = true;
                }
                builder.addStatement(")");
            }

            if (selectBox.alignment != Align.center) {
                addSetAlignmentStatement(builder, variableName, classNameGetter, selectBox.alignment);
            }

            if (selectBox.selected != 0) addSetSelectedIndexStatement(builder, variableName, selectBox.selected);
            if (selectBox.scrollingDisabled) addSetScrollingDisabledStatement(builder, variableName, true);

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimSlider) {
            var slider = (SimSlider) actor;
            if (slider.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("slider", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(%Lf, %Lf, %Lf, %L, skin%L)", variableName, classNameGetter.get(Slider.class),
                slider.minimum, slider.maximum, slider.increment, slider.vertical,
                slider.style.name.equals("default") ? "" : ", \"" + slider.style.name + "\"");

            if (slider.name != null) addSetNameStatement(builder, variableName, slider.name);

            if (!slider.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (slider.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, slider.touchable);
            }

            if (slider.disabled) addSetIsDisabledStatement(builder, variableName, true);
            if (!MathUtils.isZero(slider.value)) addSetValueStatement(builder, variableName, slider.value);
            if (!MathUtils.isZero(slider.animationDuration))
                addSetAnimationDurationStatement(builder, variableName, slider.animationDuration);

            if (slider.animateInterpolation != null && slider.animateInterpolation != Interpol.LINEAR)
                addSetAnimateInterpolationStatement(builder, variableName, classNameGetter, slider.animateInterpolation);

            if (!slider.round) addSetRoundStatement(builder, variableName, false);
            if (slider.visualInterpolation != null && slider.visualInterpolation != Interpol.LINEAR)
                addSetVisualInterpolationStatement(builder, variableName, classNameGetter, slider.visualInterpolation);

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimTextButton) {
            var textButton = (SimTextButton) actor;
            if (textButton.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("textButton", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(%S, skin%L)", variableName, classNameGetter.get(TextButton.class), convertEscapedCharacters(textButton.text),
                textButton.style.name.equals("default") ? "" : ", \"" + textButton.style.name + "\"");

            if (textButton.name != null) addSetNameStatement(builder, variableName, textButton.name);

            if (!textButton.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (textButton.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, textButton.touchable);
            }
            if (textButton.checked) addSetIsCheckedStatement(builder, variableName, true);
            if (textButton.disabled) addSetIsDisabledStatement(builder, variableName, true);
            if (textButton.color != null) addSetColorStatement(builder, variableName, textButton.color);

            if (!Utils.isEqual(0, textButton.padLeft, textButton.padRight, textButton.padTop, textButton.padBottom)) {
                addPadStatements(builder, variableName, textButton.padLeft, textButton.padRight, textButton.padTop, textButton.padBottom);
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimTextField) {
            var textField = (SimTextField) actor;
            if (textField.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("textField", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(%S, skin%L)", variableName, classNameGetter.get(TextField.class), convertEscapedCharacters(textField.text),
                textField.style.name.equals("default") ? "" : ", \"" + textField.style.name + "\"");

            if (textField.name != null) addSetNameStatement(builder, variableName, textField.name);

            if (!textField.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (textField.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, textField.touchable);
            }

            if (textField.passwordCharacter != '•')
                addSetPasswordCharacterStatement(builder, variableName, textField.passwordCharacter);
            if (textField.passwordMode) addSetPasswordModeStatement(builder, variableName, true);

            if (textField.alignment != Align.left) {
                addSetAlignmentStatement(builder, variableName, classNameGetter, textField.alignment);
            }

            if (textField.disabled) addSetIsDisabledStatement(builder, variableName, true);
            if (textField.cursorPosition != 0)
                addSetCursorPositionStatement(builder, variableName, textField.cursorPosition);

            if (textField.selectAll) {
                addSetSelectionStatement(builder, variableName, 0, convertEscapedCharacters(textField.text).length());
            } else if (textField.selectionStart != 0 || textField.selectionEnd != 0) {
                addSetSelectionStatement(builder, variableName, textField.selectionStart, textField.selectionEnd);
            }

            if (!textField.focusTraversal) addSetFocusTraversalStatement(builder, variableName, false);
            if (textField.maxLength != 0) addSetMaxLengthStatement(builder, variableName, textField.maxLength);
            if (textField.messageText != null) addSetMessageTextStatement(builder, variableName, textField.messageText);

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimTextArea) {
            var textArea = (SimTextArea) actor;
            if (textArea.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("textArea", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(%S, skin%L)", variableName, classNameGetter.get(TextArea.class), convertEscapedCharacters(textArea.text),
                textArea.style.name.equals("default") ? "" : ", \"" + textArea.style.name + "\"");

            if (textArea.name != null) addSetNameStatement(builder, variableName, textArea.name);

            if (!textArea.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (textArea.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, textArea.touchable);
            }

            if (textArea.passwordCharacter != '•')
                addSetPasswordCharacterStatement(builder, variableName, textArea.passwordCharacter);
            if (textArea.passwordMode) addSetPasswordModeStatement(builder, variableName, true);

            if (textArea.alignment != Align.left) {
                addSetAlignmentStatement(builder, variableName, classNameGetter, textArea.alignment);
            }

            if (textArea.disabled) addSetIsDisabledStatement(builder, variableName, true);
            if (textArea.cursorPosition != 0)
                addSetCursorPositionStatement(builder, variableName, textArea.cursorPosition);

            if (textArea.selectAll) {
                addSetSelectionStatement(builder, variableName, 0, convertEscapedCharacters(textArea.text).length());
            } else if (textArea.selectionStart != 0 || textArea.selectionEnd != 0) {
                addSetSelectionStatement(builder, variableName, textArea.selectionStart, textArea.selectionEnd);
            }

            if (!textArea.focusTraversal) addSetFocusTraversalStatement(builder, variableName, false);
            if (textArea.maxLength != 0) addSetMaxLengthStatement(builder, variableName, textArea.maxLength);
            if (textArea.messageText != null) addSetMessageTextStatement(builder, variableName, textArea.messageText);
            if (textArea.preferredRows > 0) addSetPrefRowsStatement(builder, variableName, textArea.preferredRows);

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimTouchPad) {
            var touchPad = (SimTouchPad) actor;
            if (touchPad.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("touchPad", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(%Lf, skin%L)", variableName, classNameGetter.get(Touchpad.class), touchPad.deadZone,
                touchPad.style.name.equals("default") ? "" : ", \"" + touchPad.style.name + "\"");

            if (touchPad.name != null) addSetNameStatement(builder, variableName, touchPad.name);

            if (!touchPad.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (touchPad.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, touchPad.touchable);
            }

            if (!touchPad.resetOnTouchUp) addSetResetOnTouchUpStatement(builder, variableName, false);

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimContainer) {
            var container = (SimContainer) actor;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("container", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T<>()", variableName, classNameGetter.get(Container.class));

            if (container.name != null) addSetNameStatement(builder, variableName, container.name);

            if (!container.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (container.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, container.touchable);
            }

            if (container.alignment != Align.center) {
                addAlignStatement(builder, variableName, classNameGetter, container.alignment);
            }

            addFillStatements(builder, variableName, container.fillX, container.fillY);
            addWidthAndHeightStatements(builder, variableName, container.minWidth, container.minHeight, container.maxWidth, container.maxHeight, container.preferredWidth, container.preferredHeight);

            if (!Utils.isEqual(0, container.padLeft, container.padRight, container.padTop, container.padBottom)) {
                addPadStatements(builder, variableName, container.padLeft, container.padRight, container.padTop, container.padBottom);
            }

            WidgetNamePair pair = createWidget(container.child, variables, usedVariables, classNameGetter);
            if (pair != null) {
                builder.add("\n");
                builder.add(pair.codeBlock);
                addSetActorStatement(builder, variableName, pair);
                variables.removeValue(pair.name, false);
                usedVariables.add(pair.name);
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimHorizontalGroup) {
            var horizontalGroup = (SimHorizontalGroup) actor;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("horizontalGroup", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T()", variableName, classNameGetter.get(HorizontalGroup.class));

            if (horizontalGroup.name != null) addSetNameStatement(builder, variableName, horizontalGroup.name);

            if (!horizontalGroup.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (horizontalGroup.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, horizontalGroup.touchable);
            }

            if (horizontalGroup.alignment != Align.center) {
                addAlignStatement(builder, variableName, classNameGetter, horizontalGroup.alignment);
            }

            if (horizontalGroup.expand) addExpandStatement(builder, variableName);
            if (horizontalGroup.fill) addFillStatement(builder, variableName);

            if (!Utils.isEqual(0, horizontalGroup.padLeft, horizontalGroup.padRight, horizontalGroup.padTop, horizontalGroup.padBottom)) {
                addPadStatements(builder, variableName, horizontalGroup.padLeft, horizontalGroup.padRight, horizontalGroup.padTop, horizontalGroup.padBottom);
            }

            if (horizontalGroup.reverse) addReverseStatement(builder, variableName);

            if (horizontalGroup.rowAlignment != Align.center) {
                addRowAlignStatement(builder, variableName, classNameGetter, horizontalGroup.rowAlignment);
            }

            if (!MathUtils.isZero(horizontalGroup.space))
                addSpaceStatement(builder, variableName, horizontalGroup.space);
            if (horizontalGroup.wrap) addWrapStatement(builder, variableName);
            if (!MathUtils.isZero(horizontalGroup.wrapSpace))
                builder.addStatement("%L.wrapSpace(%Lf)", variableName, horizontalGroup.wrapSpace);

            for (var child : horizontalGroup.children) {
                WidgetNamePair pair = createWidget(child, variables, usedVariables, classNameGetter);
                if (pair != null) {
                    builder.add("\n");
                    builder.add(pair.codeBlock);
                    addAddActorStatement(builder, variableName, pair);
                    variables.removeValue(pair.name, false);
                    usedVariables.add(pair.name);
                }
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimScrollPane) {
            var scrollPane = (SimScrollPane) actor;
            if (scrollPane.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("scrollPane", variables);

            WidgetNamePair pair = createWidget(scrollPane.child, variables, usedVariables, classNameGetter);
            if (pair != null) {
                builder.add("\n");
                builder.add(pair.codeBlock);
                variables.removeValue(pair.name, false);
                usedVariables.add(pair.name);
            }
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.add("%L = %T(", variableName, classNameGetter.get(ScrollPane.class));
            if (pair != null) builder.add("%L, skin", pair.name);
            else builder.add("skin");
            builder.addStatement("%L)", scrollPane.style.name.equals("default") ? "" : ", \"" + scrollPane.style.name + "\"");

            if (scrollPane.name != null) addSetNameStatement(builder, variableName, scrollPane.name);

            if (!scrollPane.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (scrollPane.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, scrollPane.touchable);
            }

            if (!scrollPane.fadeScrollBars) addSetFadeScrollBarsStatement(builder, variableName, false);
            if (scrollPane.clamp) addSetClampStatement(builder, variableName, true);
            if (!scrollPane.flickScroll) addSetFlickScrollStatement(builder, variableName, false);
            if (!MathUtils.isEqual(scrollPane.flingTime, 1f))
                addSetFlingTimeStatement(builder, variableName, scrollPane.flingTime);

            if (scrollPane.forceScrollX || scrollPane.forceScrollY) {
                addSetForceScrollStatement(builder, variableName, scrollPane.forceScrollX, scrollPane.forceScrollY);
            }

            if (!scrollPane.overScrollX || !scrollPane.overScrollY) {
                addSetOverscrollStatement(builder, variableName, scrollPane.overScrollX, scrollPane.overScrollY);
            }

            if (!MathUtils.isEqual(50f, scrollPane.overScrollDistance) || !MathUtils.isEqual(30f, scrollPane.overScrollSpeedMin) || !MathUtils.isEqual(200f, scrollPane.overScrollSpeedMax)) {
                addSetupOverscrollStatement(builder, variableName, scrollPane.overScrollDistance, scrollPane.overScrollSpeedMin, scrollPane.overScrollSpeedMax);
            }

            if (!scrollPane.scrollBarBottom || !scrollPane.scrollBarRight) {
                addSetScrollBarPositionsStatement(builder, variableName, scrollPane.scrollBarBottom, scrollPane.scrollBarRight);
            }

            if (scrollPane.scrollBarsOnTop) addSetScrollbarsOnTopStatement(builder, variableName, true);
            if (!scrollPane.scrollBarsVisible) addSetScrollbarsVisibleStatement(builder, variableName, false);
            if (!scrollPane.scrollBarTouch) addSetScrollBarTouchStatement(builder, variableName, false);

            if (scrollPane.scrollingDisabledX || scrollPane.scrollingDisabledY) {
                addSetScrollingDisabledStatement(builder, variableName, scrollPane.scrollingDisabledX, scrollPane.scrollingDisabledY);
            }

            if (!scrollPane.smoothScrolling) addSetSmoothScrollingStatement(builder, variableName, false);
            if (!scrollPane.variableSizeKnobs) addSetVariableSizeKnobsStatement(builder, variableName, false);

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimStack) {
            var stack = (SimStack) actor;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("stack", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T()", variableName, classNameGetter.get(Stack.class));

            if (stack.name != null) addSetNameStatement(builder, variableName, stack.name);

            if (!stack.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (stack.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, stack.touchable);
            }

            for (var child : stack.children) {
                WidgetNamePair pair = createWidget(child, variables, usedVariables, classNameGetter);
                if (pair != null) {
                    builder.add("\n");
                    builder.add(pair.codeBlock);
                    addAddActorStatement(builder, variableName, pair);
                    variables.removeValue(pair.name, false);
                    usedVariables.add(pair.name);
                }
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimSplitPane) {
            var splitPane = (SimSplitPane) actor;
            if (splitPane.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("splitPane", variables);

            WidgetNamePair pair1 = createWidget(splitPane.childFirst, variables, usedVariables, classNameGetter);
            if (pair1 != null) {
                builder.add("\n");
                builder.add(pair1.codeBlock);
                variables.removeValue(pair1.name, false);
                usedVariables.add(pair1.name);
            }
            WidgetNamePair pair2 = createWidget(splitPane.childSecond, variables, usedVariables, classNameGetter);
            if (pair2 != null) {
                builder.add("\n");
                builder.add(pair2.codeBlock);
                variables.removeValue(pair2.name, false);
                usedVariables.add(pair2.name);
            }
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.add("%L = %T(", variableName, classNameGetter.get(SplitPane.class))
                .add("%L, %L, %L, skin", pair1 == null ? null : pair1.name, pair2 == null ? null : pair2.name, splitPane.vertical)
                .addStatement("%L)", splitPane.style.name.equals("default-horizontal") || splitPane.style.name.equals("default-vertical") ? "" : ", \"" + splitPane.style.name + "\"");

            if (splitPane.name != null) addSetNameStatement(builder, variableName, splitPane.name);

            if (!splitPane.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (splitPane.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, splitPane.touchable);
            }

            if (!MathUtils.isEqual(.5f, splitPane.split))
                addSetSplitAmountStatement(builder, variableName, splitPane.split);
            if (!MathUtils.isZero(splitPane.splitMin))
                addSetMinSplitAmountStatement(builder, variableName, splitPane.splitMin);
            if (!MathUtils.isEqual(1, splitPane.splitMax))
                addSetMaxSplitAmountStatement(builder, variableName, splitPane.splitMax);

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimTree) {
            var tree = (SimTree) actor;
            if (tree.style == null) return null;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("tree", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T<%T, %T>(skin%L)", variableName, classNameGetter.get(Tree.class),
                nodeClassName, classNameGetter.get(String.class),
                tree.style.name.equals("default") ? "" : ", \"" + tree.style.name + "\"");

            if (tree.name != null) addSetNameStatement(builder, variableName, tree.name);

            if (!tree.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (tree.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, tree.touchable);
            }

            if (!MathUtils.isZero(tree.padLeft) || !MathUtils.isZero(tree.padRight)) {
                if (MathUtils.isEqual(tree.padLeft, tree.padRight)) {
                    addSetPaddingStatement(builder, variableName, tree.padLeft);
                } else {
                    addSetPaddingStatement(builder, variableName, tree.padLeft, tree.padRight);
                }
            }

            if (!MathUtils.isEqual(2f, tree.iconSpaceLeft) || !MathUtils.isZero(2f, tree.iconSpaceRight)) {
                addSetIconSpacingStatement(builder, variableName, tree.iconSpaceLeft, tree.iconSpaceRight);
            }

            if (!MathUtils.isZero(tree.indentSpacing))
                addSetIndentSpacingStatement(builder, variableName, tree.indentSpacing);
            if (!MathUtils.isEqual(4f, tree.ySpacing)) addSetYSpacingStatement(builder, variableName, tree.ySpacing);

            for (var node : tree.children) {
                var pair = createWidget(node, variables, usedVariables, classNameGetter);
                if (pair != null) {
                    builder.add("\n");
                    builder.add(pair.codeBlock);
                    addAddNodeStatement(builder, variableName, pair);
                    variables.removeValue(pair.name, false);
                    usedVariables.add(pair.name);
                }
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimNode) {
            var node = (SimNode) actor;

            WidgetNamePair pair = createWidget(node.actor, variables, usedVariables, classNameGetter);
            if (pair == null) return null;

            var builder = CodeBlock.builder();
            builder.add("\n");
            builder.add(pair.codeBlock);

            var variableName = createVariableName("node", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T(%L)", variableName, nodeClassName, pair.name);
            variables.removeValue(pair.name, false);
            usedVariables.add(pair.name);
            if (node.icon != null)
                addSetIconStatement(builder, variableName, node.icon);
            if (!node.selectable) addSetSelectableStatement(builder, variableName, false);
            if (node.expanded) addSetExpandedStatement(builder, variableName, true);

            for (var child : node.nodes) {
                var nodePair = createWidget(child, variables, usedVariables, classNameGetter);
                if (nodePair != null) {
                    builder.add("\n");
                    builder.add(nodePair.codeBlock);
                    addAddNodeStatement(builder, variableName, nodePair);
                    variables.removeValue(nodePair.name, false);
                    usedVariables.add(nodePair.name);
                }
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else if (actor instanceof SimVerticalGroup) {
            var verticalGroup = (SimVerticalGroup) actor;

            var builder = CodeBlock.builder();
            var variableName = createVariableName("verticalGroup", variables);
            if (!usedVariables.contains(variableName)) builder.add("var ");
            builder.addStatement("%L = %T()", variableName, classNameGetter.get(VerticalGroup.class));

            if (verticalGroup.name != null) addSetNameStatement(builder, variableName, verticalGroup.name);

            if (!verticalGroup.visible) {
                addSetTouchableStatement(builder, variableName, classNameGetter, Touchable.disabled);
            } else if (verticalGroup.touchable != Touchable.enabled) {
                addSetTouchableStatement(builder, variableName, classNameGetter, verticalGroup.touchable);
            }

            if (verticalGroup.alignment != Align.center) {
                addAlignStatement(builder, variableName, classNameGetter, verticalGroup.alignment);
            }

            if (verticalGroup.expand) addExpandStatement(builder, variableName);
            if (verticalGroup.fill) addFillStatement(builder, variableName);

            if (!Utils.isEqual(0, verticalGroup.padLeft, verticalGroup.padRight, verticalGroup.padTop, verticalGroup.padBottom)) {
                addPadStatements(builder, variableName, verticalGroup.padLeft, verticalGroup.padRight, verticalGroup.padTop, verticalGroup.padBottom);
            }

            if (verticalGroup.reverse) addReverseStatement(builder, variableName);

            if (verticalGroup.columnAlignment != Align.center) {
                addColumnAlignStatement(builder, variableName, classNameGetter, verticalGroup.columnAlignment);
            }

            if (!MathUtils.isZero(verticalGroup.space)) addSpaceStatement(builder, variableName, verticalGroup.space);
            if (verticalGroup.wrap) addWrapStatement(builder, variableName);
            if (!MathUtils.isZero(verticalGroup.wrapSpace))
                addWrapSpaceStatement(builder, variableName, verticalGroup.wrapSpace);

            for (var child : verticalGroup.children) {
                WidgetNamePair pair = createWidget(child, variables, usedVariables, classNameGetter);
                if (pair != null) {
                    builder.add("\n");
                    builder.add(pair.codeBlock);
                    addAddActorStatement(builder, variableName, pair);
                    variables.removeValue(pair.name, false);
                    usedVariables.add(pair.name);
                }
            }

            return new WidgetNamePair(builder.build(), variableName);
        } else {
            return null;
        }
    }

    private static void addSetSelectedIndexStatement(CodeBlock.Builder builder, String variableName, int selected) {
        builder.addStatement("%L.setSelectedIndex(%L)", variableName, selected);
    }

    private static void addSetMaxListCountStatement(CodeBlock.Builder builder, String variableName, int maxListCount) {
        builder.addStatement("%L.maxListCount = %L", variableName, maxListCount);
    }

    private static void addSetNameStatement(CodeBlock.Builder builder, String variableName, String name) {
        builder.addStatement("%L.name = %S", variableName, name);
    }

    private static void addSetTouchableStatement(CodeBlock.Builder builder, String variableName, ClassNameGetter classNameGetter, Touchable touchable) {
        builder.addStatement("%L.touchable = %T.%L", variableName, classNameGetter.get(Touchable.class), touchable);
    }

    private static void addSetBackgroundStatement(CodeBlock.Builder builder, String variableName, DrawableData background) {
        builder.addStatement("%L.background = skin.getDrawable(%S)", variableName, background.name);
    }

    private static void addSetColorStatement(CodeBlock.Builder builder, String variableName, ColorData color) {
        builder.addStatement("%L.color = skin.getColor(%S)", variableName, color.getName());
    }

    private static void addWrapSpaceStatement(CodeBlock.Builder builder, String variableName, float wrapSpace) {
        builder.addStatement("%L.wrapSpace(%Lf)", variableName, wrapSpace);
    }

    private static void addSetSelectableStatement(CodeBlock.Builder builder, String variableName, boolean selectable) {
        builder.addStatement("%L.isSelectable = %L", variableName, selectable);
    }

    private static void addSetExpandedStatement(CodeBlock.Builder builder, String variableName, boolean expanded) {
        builder.addStatement("%L.isExpanded = %L", variableName, expanded);
    }

    private static void addSetIconStatement(CodeBlock.Builder builder, String variableName, DrawableData icon) {
        builder.addStatement("%L.icon = skin.getDrawable(%S)", variableName, icon.name);
    }

    private static void addAddNodeStatement(CodeBlock.Builder builder, String variableName, WidgetNamePair pair) {
        builder.addStatement("%L.add(%L)", variableName, pair.name);
    }

    private static void addSetYSpacingStatement(CodeBlock.Builder builder, String variableName, float ySpacing) {
        builder.addStatement("%L.ySpacing = %Lf", variableName, ySpacing);
    }

    private static void addSetIndentSpacingStatement(CodeBlock.Builder builder, String variableName, float indentSpacing) {
        builder.addStatement("%L.indentSpacing = %Lf", variableName, indentSpacing);
    }

    private static void addSetIconSpacingStatement(CodeBlock.Builder builder, String variableName, float iconSpaceLeft, float iconSpaceRight) {
        builder.addStatement("%L.setIconSpacing(%Lf, %Lf)", variableName, iconSpaceLeft, iconSpaceRight);
    }

    private static void addSetPaddingStatement(CodeBlock.Builder builder, String variableName, float padding) {
        builder.addStatement("%L.setPadding(%Lf)", variableName, padding);
    }

    private static void addSetPaddingStatement(CodeBlock.Builder builder, String variableName, float padLeft, float padRight) {
        builder.addStatement("%L.setPadding(%Lf, %Lf)", variableName, padLeft, padRight);
    }

    private static void addPadStatements(CodeBlock.Builder builder, String variableName, float padLeft, float padRight, float padTop, float padBottom) {
        if (Utils.isEqual(padLeft, padRight, padTop, padBottom)) {
            builder.addStatement("%L.pad(%Lf)", variableName, padLeft);
        } else {
            builder.addStatement("%L.padLeft(%Lf)", variableName, padLeft);
            builder.addStatement("%L.padRight(%Lf)", variableName, padRight);
            builder.addStatement("%L.padTop(%Lf)", variableName, padTop);
            builder.addStatement("%L.padBottom(%Lf)", variableName, padBottom);
        }
    }

    private static void addSetSplitAmountStatement(CodeBlock.Builder builder, String variableName, float split) {
        builder.addStatement("%L.splitAmount = %L", variableName, split);
    }

    private static void addSetMinSplitAmountStatement(CodeBlock.Builder builder, String variableName, float splitMin) {
        builder.addStatement("%L.minSplitAmount = %L", variableName, splitMin);
    }

    private static void addSetMaxSplitAmountStatement(CodeBlock.Builder builder, String variableName, float splitMax) {
        builder.addStatement("%L.maxSplitAmount = %L", variableName, splitMax);
    }

    private static void addAddActorStatement(CodeBlock.Builder builder, String variableName, WidgetNamePair pair) {
        builder.addStatement("%L.addActor(%L)", variableName, pair.name);
    }

    private static void addSetVariableSizeKnobsStatement(CodeBlock.Builder builder, String variableName, boolean variableSizeKnobs) {
        builder.addStatement("%L.variableSizeKnobs = %L", variableName, variableSizeKnobs);
    }

    private static void addSetSmoothScrollingStatement(CodeBlock.Builder builder, String variableName, boolean smoothScrolling) {
        builder.addStatement("%L.setSmoothScrolling(%L)", variableName, smoothScrolling);
    }

    private static void addSetScrollingDisabledStatement(CodeBlock.Builder builder, String variableName, boolean scrollingDisabledX, boolean scrollingDisabledY) {
        builder.addStatement("%L.setScrollingDisabled(%L, %L)", variableName, scrollingDisabledX, scrollingDisabledY);
    }

    private static void addSetScrollingDisabledStatement(CodeBlock.Builder builder, String variableName, boolean scrollingDisabled) {
        builder.addStatement("%L.setScrollingDisabled(%L)", variableName, scrollingDisabled);
    }

    private static void addSetScrollBarTouchStatement(CodeBlock.Builder builder, String variableName, boolean touchable) {
        builder.addStatement("%L.setScrollBarTouch(%L)", variableName, touchable);
    }

    private static void addSetScrollbarsVisibleStatement(CodeBlock.Builder builder, String variableName, boolean visible) {
        builder.addStatement("%L.setScrollbarsVisible(%L)", variableName, visible);
    }

    private static void addSetScrollbarsOnTopStatement(CodeBlock.Builder builder, String variableName, boolean scrollBarsOnTop) {
        builder.addStatement("%L.setScrollbarsOnTop(%L)", variableName, scrollBarsOnTop);
    }

    private static void addSetScrollBarPositionsStatement(CodeBlock.Builder builder, String variableName, boolean scrollBarBottom, boolean scrollBarRight) {
        builder.addStatement("%L.setScrollBarPositions(%L, %L)", variableName, scrollBarBottom, scrollBarRight);
    }

    private static void addSetupOverscrollStatement(CodeBlock.Builder builder, String variableName, float overScrollDistance, float overScrollSpeedMin, float overScrollSpeedMax) {
        builder.addStatement("%L.setupOverscroll(%Lf, %Lf, %Lf)", variableName, overScrollDistance, overScrollSpeedMin, overScrollSpeedMax);
    }

    private static void addSetOverscrollStatement(CodeBlock.Builder builder, String variableName, boolean overScrollX, boolean overScrollY) {
        builder.addStatement("%L.setOverscroll(%L, %L)", variableName, overScrollX, overScrollY);
    }

    private static void addSetForceScrollStatement(CodeBlock.Builder builder, String variableName, boolean forceScrollX, boolean forceScrollY) {
        builder.addStatement("%L.setForceScroll(%L, %L)", variableName, forceScrollX, forceScrollY);
    }

    private static void addSetFlingTimeStatement(CodeBlock.Builder builder, String variableName, float flingTime) {
        builder.addStatement("%L.setFlingTime(%Lf)", variableName, flingTime);
    }

    private static void addSetFlickScrollStatement(CodeBlock.Builder builder, String variableName, boolean flickScroll) {
        builder.addStatement("%L.setFlickScroll(%L)", variableName, flickScroll);
    }

    private static void addSetClampStatement(CodeBlock.Builder builder, String variableName, boolean clamp) {
        builder.addStatement("%L.setClamp(%L)", variableName, clamp);
    }

    private static void addSetFadeScrollBarsStatement(CodeBlock.Builder builder, String variableName, boolean fadeScrollBars) {
        builder.addStatement("%L.fadeScrollBars = %L", variableName, fadeScrollBars);
    }

    private static void addSpaceStatement(CodeBlock.Builder builder, String variableName, float space) {
        builder.addStatement("%L.space(%Lf)", variableName, space);
    }

    private static void addReverseStatement(CodeBlock.Builder builder, String variableName) {
        builder.addStatement("%L.reverse()", variableName);
    }

    private static void addFillStatement(CodeBlock.Builder builder, String variableName) {
        builder.addStatement("%L.fill()", variableName);
    }

    private static void addExpandStatement(CodeBlock.Builder builder, String variableName) {
        builder.addStatement("%L.expand()", variableName);
    }

    private static void addSetActorStatement(CodeBlock.Builder builder, String variableName, WidgetNamePair pair) {
        builder.addStatement("%L.actor = %L", variableName, pair.name);
    }

    private static void addWidthAndHeightStatements(CodeBlock.Builder builder, String variableName, float minWidth, float minHeight, float maxWidth, float maxHeight, float preferredWidth, float preferredHeight) {
        boolean hasMinWidth = false, hasMinHeight = false, hasMaxWidth = false, hasMaxHeight = false, hasPreferredWidth = false, hasPreferredHeight = false;
        if (Utils.isEqual(minWidth, minHeight, maxWidth, maxHeight, preferredWidth,
            preferredHeight) && !Utils.isEqual(-1, minWidth)) {
            builder.addStatement("%L.size(%Lf)", variableName, minWidth);
            hasMinWidth = true;
            hasMinHeight = true;
            hasMaxWidth = true;
            hasMaxHeight = true;
            hasPreferredWidth = true;
            hasPreferredHeight = true;
        }
        if (!hasMinWidth && Utils.isEqual(minWidth, maxWidth, preferredWidth) && !Utils.isEqual(-1, minWidth)) {
            builder.addStatement("%L.width(%Lf)", variableName, minWidth);
            hasMinWidth = true;
            hasMaxWidth = true;
            hasPreferredWidth = true;
        }
        if (!hasMinHeight && Utils.isEqual(minHeight, maxHeight, preferredHeight) && !Utils.isEqual(-1, minHeight)) {
            builder.addStatement("%L.height(%Lf)", variableName, minHeight);
            hasMinHeight = true;
            hasMaxHeight = true;
            hasPreferredHeight = true;
        }
        if (!hasMinWidth && !hasMinHeight && Utils.isEqual(minWidth, minHeight) && !Utils.isEqual(-1, minWidth)) {
            builder.addStatement("%L.minSize(%Lf)", variableName, minWidth);
            hasMinWidth = true;
            hasMinHeight = true;
        }
        if (!hasMaxWidth && !hasMaxHeight && Utils.isEqual(maxWidth, maxHeight) && !Utils.isEqual(-1, maxWidth)) {
            builder.addStatement("%L.maxSize(%Lf)", variableName, maxWidth);
            hasMaxWidth = true;
            hasMaxHeight = true;
        }
        if (!hasPreferredWidth && !hasPreferredHeight && Utils.isEqual(preferredWidth, preferredHeight) && !Utils.isEqual(-1,
            preferredWidth)) {
            builder.addStatement("%L.prefSize(%Lf)", variableName, preferredWidth);
            hasPreferredWidth = true;
            hasPreferredHeight = true;
        }
        if (!hasMinWidth && !Utils.isEqual(-1, minWidth)) {
            builder.addStatement("%L.minWidth(%Lf)", variableName, minWidth);
        }
        if (!hasMinHeight && !Utils.isEqual(-1, minHeight)) {
            builder.addStatement("%L.minHeight(%Lf)", variableName, minHeight);
        }
        if (!hasMaxWidth && !Utils.isEqual(-1, maxWidth)) {
            builder.addStatement("%L.maxWidth(%Lf)", variableName, maxWidth);
        }
        if (!hasMaxHeight && !Utils.isEqual(-1, maxHeight)) {
            builder.addStatement("%L.maxHeight(%Lf)", variableName, maxHeight);
        }
        if (!hasPreferredWidth && !Utils.isEqual(-1, preferredWidth)) {
            builder.addStatement("%L.prefWidth(%Lf)", variableName, preferredWidth);
        }
        if (!hasPreferredHeight && !Utils.isEqual(-1, preferredHeight)) {
            builder.addStatement("%L.prefHeight(%Lf)", variableName, preferredHeight);
        }
    }

    private static void addFillStatements(CodeBlock.Builder builder, String variableName, boolean fillX, boolean fillY) {
        if (fillX && fillY) {
            builder.addStatement("%L.fill()", variableName);
        } else if (fillX) {
            builder.addStatement("%L.fillX()", variableName);
        } else if (fillY) {
            builder.addStatement("%L.fill()", variableName);
        }
    }

    private static void addSetResetOnTouchUpStatement(CodeBlock.Builder builder, String variableName, boolean resetOnTouchUp) {
        builder.addStatement("%L.resetOnTouchUp = %L", variableName, resetOnTouchUp);
    }

    private static void addSetPrefRowsStatement(CodeBlock.Builder builder, String variableName, int preferredRows) {
        builder.addStatement("%L.setPrefRows(%L)", variableName, preferredRows);
    }

    private static void addSetPasswordModeStatement(CodeBlock.Builder builder, String variableName, boolean passwordMode) {
        builder.addStatement("%L.isPasswordMode = %L", variableName, passwordMode);
    }

    private static void addSetMessageTextStatement(CodeBlock.Builder builder, String variableName, String messageText) {
        builder.addStatement("%L.messageText = %S", variableName, messageText);
    }

    private static void addSetMaxLengthStatement(CodeBlock.Builder builder, String variableName, int maxLength) {
        builder.addStatement("%L.maxLength = %L", variableName, maxLength);
    }

    private static void addSetFocusTraversalStatement(CodeBlock.Builder builder, String variableName, boolean focusTraversal) {
        builder.addStatement("%L.setFocusTraversal(%L)", variableName, focusTraversal);
    }

    private static void addSetSelectionStatement(CodeBlock.Builder builder, String variableName, int from, int to) {
        builder.addStatement("%L.setSelection(%L, %L)", variableName, from, to);
    }

    private static void addSetCursorPositionStatement(CodeBlock.Builder builder, String variableName, int cursorPosition) {
        builder.addStatement("%L.cursorPosition = %L", variableName, cursorPosition);
    }

    private static void addSetPasswordCharacterStatement(CodeBlock.Builder builder, String variableName, char passwordCharacter) {
        builder.addStatement("%L.setPasswordCharacter('%L')", variableName, passwordCharacter);
    }

    private static void addSetVisualInterpolationStatement(CodeBlock.Builder builder, String variableName, ClassNameGetter classNameGetter, Interpol visualInterpolation) {
        builder.addStatement("%L.setVisualInterpolation(%T.%L)", variableName,
            classNameGetter.get(Interpolation.class), visualInterpolation.code);
    }

    private static void addSetRoundStatement(CodeBlock.Builder builder, String variableName, boolean round) {
        builder.addStatement("%L.setRound(%L)", variableName, round);
    }

    private static void addSetAnimateInterpolationStatement(CodeBlock.Builder builder, String variableName, ClassNameGetter classNameGetter, Interpol animateInterpolation) {
        builder.addStatement("%L.setAnimateInterpolation(%T.%L)", variableName,
            classNameGetter.get(Interpolation.class), animateInterpolation.code);
    }

    private static void addSetAnimationDurationStatement(CodeBlock.Builder builder, String variableName, float animationDuration) {
        builder.addStatement("%L.setAnimationDuration(%Lf)", variableName, animationDuration);
    }

    private static void addSetValueStatement(CodeBlock.Builder builder, String variableName, float value) {
        builder.addStatement("%L.setValue(%Lf)", variableName, value);
    }

    private static void addSetWrapStatement(CodeBlock.Builder builder, String variableName, boolean wrap) {
        builder.addStatement("%L.wrap = %L", variableName, wrap);
    }

    private static void addWrapStatement(CodeBlock.Builder builder, String variableName) {
        builder.addStatement("%L.wrap()", variableName);
    }

    private static void addSetEllipsisStatement(CodeBlock.Builder builder, String variableName, boolean ellipsis) {
        builder.addStatement("%L.setEllipsis(%L)", variableName, ellipsis);
    }

    private static void addSetEllipsisStatement(CodeBlock.Builder builder, String variableName, String ellipsisText) {
        builder.addStatement("%L.setEllipsis(%S)", variableName, ellipsisText);
    }

    private static void addSetAlignmentStatement(CodeBlock.Builder builder, String variableName, ClassNameGetter classNameGetter, int alignment) {
        builder.addStatement("%L.setAlignment(%T.%L)", variableName, classNameGetter.get(Align.class), alignmentToName(alignment));
    }

    private static void addSetScalingStatement(CodeBlock.Builder builder, String variableName, ClassNameGetter classNameGetter, String scaling) {
        builder.addStatement("%L.setScaling(%T.%L)", variableName, classNameGetter.get(Scaling.class), scaling);
    }

    private static void addSetIsDisabledStatement(CodeBlock.Builder builder, String variableName, boolean disabled) {
        builder.addStatement("%L.isDisabled = %L", variableName, disabled);
    }

    private static void addSetIsCheckedStatement(CodeBlock.Builder builder, String variableName, boolean checked) {
        builder.addStatement("%L.isChecked = %L", variableName, checked);
    }

    private static void addAlignStatement(CodeBlock.Builder builder, String variableName, ClassNameGetter classNameGetter, int alignment) {
        builder.addStatement("%L.align(%T.%L)", variableName, classNameGetter.get(Align.class), alignmentToName(alignment));
    }

    private static void addRowAlignStatement(CodeBlock.Builder builder, String variableName, ClassNameGetter classNameGetter, int alignment) {
        builder.addStatement("%L.rowAlign(%T.%L)", variableName, classNameGetter.get(Align.class), alignmentToName(alignment));
    }

    private static void addColumnAlignStatement(CodeBlock.Builder builder, String variableName, ClassNameGetter classNameGetter, int alignment) {
        builder.addStatement("%L.columnAlign(%T.%L)", variableName, classNameGetter.get(Align.class), alignmentToName(alignment));
    }

    private static void addFSetFillParentStatement(CodeBlock.Builder builder, String variableName) {
        builder.addStatement("%L.setFillParent(true)", variableName);
    }

    private static void addChildColSpanStatement(CodeBlock.Builder builder, int colSpan) {
        builder.add(".colspan(%L)", colSpan);
    }

    private static void addChildPaddingStatements(CodeBlock.Builder builder, float padLeft, float padRight, float padTop, float padBottom) {
        if (Utils.isEqual(0, padLeft, padRight, padTop, padBottom)) return;

        if (Utils.isEqual(padLeft, padRight, padTop, padBottom)) {
            builder.add(".pad(%Lf)", padLeft);
        } else {
            if (!MathUtils.isZero(padLeft)) {
                builder.add(".padLeft(%Lf)", padLeft);
            }
            if (!MathUtils.isZero(padRight)) {
                builder.add(".padRight(%Lf)", padRight);
            }
            if (!MathUtils.isZero(padTop)) {
                builder.add(".padTop(%Lf)", padTop);
            }
            if (!MathUtils.isZero(padBottom)) {
                builder.add(".padBottom(%Lf)", padBottom);
            }
        }
    }

    private static void addChildSpaceStatements(CodeBlock.Builder builder, float spaceLeft, float spaceRight, float spaceTop, float spaceBottom) {
        if (Utils.isEqual(0, spaceLeft, spaceRight, spaceTop, spaceBottom)) return;

        if (Utils.isEqual(spaceLeft, spaceRight, spaceTop, spaceBottom)) {
            builder.add(".space(%Lf)", spaceLeft);
        } else {
            if (!MathUtils.isZero(spaceLeft)) {
                builder.add(".spaceLeft(%Lf)", spaceLeft);
            }
            if (!MathUtils.isZero(spaceRight)) {
                builder.add(".spaceRight(%Lf)", spaceRight);
            }
            if (!MathUtils.isZero(spaceTop)) {
                builder.add(".spaceTop(%Lf)", spaceTop);
            }
            if (!MathUtils.isZero(spaceBottom)) {
                builder.add(".spaceBottom(%Lf)", spaceBottom);
            }
        }
    }

    private static void addChildGrowExpandAndFillStatements(CodeBlock.Builder builder, boolean growX, boolean growY, boolean expandX, boolean expandY, boolean fillX, boolean fillY) {
        if (growX || growY) {
            if (growX && growY) {
                builder.add(".grow()");
            } else if (growX) {
                builder.add(".growX()");
            } else {
                builder.add(".growY()");
            }
        } else {
            if (expandX && expandY) {
                builder.add(".expand()");
            } else if (expandX) {
                builder.add(".expandX()");
            } else if (expandY) {
                builder.add(".expandY()");
            }

            if (fillX && fillY) {
                builder.add(".fill()");
            } else if (fillX) {
                builder.add(".fillX()");
            } else if (fillY) {
                builder.add(".fill()");
            }
        }
    }

    private static void addChildAlignStatement(CodeBlock.Builder builder, ClassNameGetter classNameGetter, int alignment) {
        if (alignment != Align.center) {
            builder.add(".align(%T.%L)", classNameGetter.get(Align.class), alignmentToName(alignment));
        }
    }

    private static void addChildWidthHeightAndSizeStatements(CodeBlock.Builder builder, float minWidth, float minHeight, float maxWidth, float maxHeight, float preferredWidth, float preferredHeight) {
        boolean hasMinWidth = false, hasMinHeight = false, hasMaxWidth = false, hasMaxHeight = false, hasPreferredWidth = false, hasPreferredHeight = false;
        if (Utils.isEqual(minWidth, minHeight, maxWidth, maxHeight, preferredWidth,
            preferredHeight) && !Utils.isEqual(-1, minWidth)) {
            builder.add(".size(%L)", minWidth);
            hasMinWidth = true;
            hasMinHeight = true;
            hasMaxWidth = true;
            hasMaxHeight = true;
            hasPreferredWidth = true;
            hasPreferredHeight = true;
        }
        if (!hasMinWidth && Utils.isEqual(minWidth, maxWidth, preferredWidth) && !Utils.isEqual(-1, minWidth)) {
            builder.add(".width(%Lf)", minWidth);
            hasMinWidth = true;
            hasMaxWidth = true;
            hasPreferredWidth = true;
        }
        if (!hasMinHeight && Utils.isEqual(minHeight, maxHeight, preferredHeight) && !Utils.isEqual(-1, minHeight)) {
            builder.add(".height(%Lf)", minHeight);
            hasMinHeight = true;
            hasMaxHeight = true;
            hasPreferredHeight = true;
        }
        if (!hasMinWidth && !hasMinHeight && Utils.isEqual(minWidth, minHeight) && !Utils.isEqual(-1, minWidth)) {
            builder.add(".minSize(%Lf)", minWidth);
            hasMinWidth = true;
            hasMinHeight = true;
        }
        if (!hasMaxWidth && !hasMaxHeight && Utils.isEqual(maxWidth, maxHeight) && !Utils.isEqual(-1, maxWidth)) {
            builder.add(".maxSize(%Lf)", maxWidth);
            hasMaxWidth = true;
            hasMaxHeight = true;
        }
        if (!hasPreferredWidth && !hasPreferredHeight && Utils.isEqual(preferredWidth, preferredHeight) && !Utils.isEqual(-1,
            preferredWidth)) {
            builder.add(".prefSize(%Lf)", preferredWidth);
            hasPreferredWidth = true;
            hasPreferredHeight = true;
        }
        if (!hasMinWidth && !Utils.isEqual(-1, minWidth)) {
            builder.add(".minWidth(%Lf)", minWidth);
        }
        if (!hasMinHeight && !Utils.isEqual(-1, minHeight)) {
            builder.add(".minHeight(%Lf)", minHeight);
        }
        if (!hasMaxWidth && !Utils.isEqual(-1, maxWidth)) {
            builder.add(".maxWidth(%Lf)", maxWidth);
        }
        if (!hasMaxHeight && !Utils.isEqual(-1, maxHeight)) {
            builder.add(".maxHeight(%Lf)", maxHeight);
        }
        if (!hasPreferredWidth && !Utils.isEqual(-1, preferredWidth)) {
            builder.add(".prefWidth(%Lf)", preferredWidth);
        }
        if (!hasPreferredHeight && !Utils.isEqual(-1, preferredHeight)) {
            builder.add(".prefHeight(%Lf)", preferredHeight);
        }
    }

    private static void addChildUniformStatements(CodeBlock.Builder builder, boolean uniformX, boolean uniformY) {
        if (uniformX && uniformY) {
            builder.add(".uniform()");
        } else if (uniformX) {
            builder.add(".uniformX()");
        } else if (uniformY) {
            builder.add(".uniformY()");
        }
    }

    private static String createVariableName(String name, Array<String> variables) {
        String returnValue = name;
        int index = 0;
        while (variables.contains(returnValue, false)) {
            index++;
            returnValue = name + index;
        }
        variables.add(returnValue);
        return returnValue;
    }

    private static String alignmentToName(int align) {
        StringBuilder buffer = new StringBuilder(13);
        if ((align & Align.top) != 0)
            buffer.append("top");
        else if ((align & Align.bottom) != 0)
            buffer.append("bottom");
        else {
            if ((align & Align.left) != 0)
                buffer.append("left");
            else if ((align & Align.right) != 0)
                buffer.append("right");
            else
                buffer.append("center");
            return buffer.toString();
        }
        if ((align & Align.left) != 0)
            buffer.append("Left");
        else if ((align & Align.right) != 0)
            buffer.append("Right");
        return buffer.toString();
    }

    private static FunSpec renderMethod() {
        Color color = rootActor.backgroundColor == null ? Color.WHITE : rootActor.backgroundColor.color;
        return FunSpec.builder("render")
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("%T.gl.glClearColor(%Lf, %Lf, %Lf, %Lf)", Gdx.class, color.r, color.g, color.b, color.a)
            .addStatement("%T.gl.glClear(%T.GL_COLOR_BUFFER_BIT)", Gdx.class, GL20.class)
            .addStatement("stage.act()")
            .addStatement("stage.draw()")
            .build();
    }

    private static FunSpec resizeMethod() {
        return FunSpec.builder("resize")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("width", Integer.TYPE)
            .addParameter("height", Integer.TYPE)
            .addStatement("stage.viewport.update(width, height, true)")
            .build();
    }

    private static FunSpec disposeMethod() {
        return FunSpec.builder("dispose")
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("stage.dispose()")
            .addStatement("skin.dispose()")
            .build();
    }

    private static TypeSpec basicNodeType() {
        return TypeSpec.classBuilder("BasicNode")
            .superclass(Node.class)
            .addFunction(FunSpec.constructorBuilder()
                .addParameter("actor", Actor.class)
                .addStatement("super(actor)")
                .build())
            .build();
    }

    private interface ClassNameGetter {
        ClassName get(Class clazz);
    }

    private static class WidgetNamePair {
        CodeBlock codeBlock;
        String name;

        public WidgetNamePair(CodeBlock codeBlock, String name) {
            this.codeBlock = codeBlock;
            this.name = name;
        }
    }
}