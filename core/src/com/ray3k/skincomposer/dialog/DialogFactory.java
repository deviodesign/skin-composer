/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2020 Raymond Buckley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.ray3k.skincomposer.dialog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.ray3k.skincomposer.Main;
import com.ray3k.skincomposer.dialog.PopWelcome.WelcomeListener;
import com.ray3k.stripe.PopTable;
import com.ray3k.stripe.Spinner;
import com.ray3k.skincomposer.UndoableManager;
import com.ray3k.skincomposer.UndoableManager.DeleteStyleUndoable;
import com.ray3k.skincomposer.UndoableManager.DuplicateStyleUndoable;
import com.ray3k.skincomposer.UndoableManager.NewStyleUndoable;
import com.ray3k.skincomposer.data.*;
import com.ray3k.skincomposer.data.CustomProperty.PropertyType;
import com.ray3k.skincomposer.dialog.Dialog9Patch.Dialog9PatchListener;
import com.ray3k.skincomposer.dialog.DialogBitmapFont.DialogBitmapFontListener;
import com.ray3k.skincomposer.dialog.DialogColors.DialogColorsListener;
import com.ray3k.skincomposer.dialog.DialogCustomClass.CustomClassListener;
import com.ray3k.skincomposer.dialog.DialogCustomProperty.CustomStylePropertyListener;
import com.ray3k.skincomposer.dialog.DialogCustomStyle.CustomStyleListener;
import com.ray3k.skincomposer.dialog.DialogDrawables.DialogDrawablesListener;
import com.ray3k.skincomposer.dialog.DialogFreeTypeFont.DialogFreeTypeFontListener;
import com.ray3k.skincomposer.dialog.DialogImageFont.ImageFontListener;
import com.ray3k.skincomposer.dialog.DialogTenPatch.DialogTenPatchListener;
import com.ray3k.skincomposer.dialog.scenecomposer.DialogSceneComposer;
import static com.ray3k.skincomposer.Main.*;

public class DialogFactory {
    private static DialogFactory instance;
    private boolean showingCloseDialog;

    public DialogFactory() {
        instance = this;
        showingCloseDialog = false;
    }

    public void showDialogAbout(DialogListener listener) {
        var pop = new PopAbout();
        if (listener != null) {
            pop.addListener(listener);
        }
        pop.show(stage);
    }
    
    public DialogExport showDialogExport(DialogListener listener) {
        var dialog = new DialogExport();
        if (listener != null) {
            dialog.addListener(listener);
        }
        dialog.show(stage);
        return dialog;
    }
    
    public DialogImport showDialogImport(DialogListener listener) {
        var dialog = new DialogImport();
        if (listener != null) {
            dialog.addListener(listener);
        }
        dialog.show(stage);
        return dialog;
    }
    
    public DialogColors showDialogColors(StyleProperty styleProperty,
        DialogColors.DialogColorsListener listener, DialogListener dialogListener) {
        DialogColors dialog = new DialogColors(main, styleProperty, listener);
        if (dialogListener != null) {
            dialog.addListener(dialogListener);
        }
        dialog.setFillParent(true);
        dialog.show(stage);
        dialog.refreshTable();
        return dialog;
    }

    public DialogColors showDialogColors(CustomProperty styleProperty, DialogColorsListener listener, DialogListener dialogListener) {
        DialogColors dialog = new DialogColors(main, styleProperty, listener);
        if (dialogListener != null) {
            dialog.addListener(dialogListener);
        }
        dialog.setFillParent(true);
        dialog.show(stage);
        dialog.refreshTable();
        return dialog;
    }
    
    public DialogColors showDialogColors(StyleProperty styleProperty, DialogListener dialogListener) {
        return DialogFactory.this.showDialogColors(styleProperty, null, dialogListener);
    }
    
    public DialogColors showDialogColors(CustomProperty styleProperty, DialogListener dialogListener) {
        return DialogFactory.this.showDialogColors(styleProperty, null, dialogListener);
    }

    public DialogColors showDialogColors(DialogListener dialogListener) {
        return DialogFactory.this.showDialogColors((StyleProperty)null, dialogListener);
    }
    
    public DialogColors showDialogColors(DialogColorsListener listener, DialogListener dialogListener) {
        return DialogFactory.this.showDialogColors((StyleProperty)null, listener, dialogListener);
    }

    public DialogDrawables showDialogDrawables(StyleProperty property,
            DialogDrawablesListener listener, DialogListener dialogListener) {
        DialogDrawables dialog = new DialogDrawables(main, property, listener);
        dialog.setFillParent(true);
        if (dialogListener != null) {
            dialog.addListener(dialogListener);
        }
        dialog.show(stage);
        return dialog;
    }
    
    public DialogDrawables showDialogDrawables(CustomProperty property,
            DialogDrawablesListener listener, DialogListener dialogListener) {
        DialogDrawables dialog = new DialogDrawables(main, property, listener);
        if (dialogListener != null) {
            dialog.addListener(dialogListener);
        }
        dialog.setFillParent(true);
        dialog.show(stage);
        return dialog;
    }

    public DialogDrawables showDialogDrawables(StyleProperty property, DialogListener dialogListener) {
        return showDialogDrawables(property, null, dialogListener);
    }
    
    public DialogDrawables showDialogDrawables(CustomProperty property, DialogListener dialogListener) {
        return showDialogDrawables(property, null, dialogListener);
    }

    public DialogDrawables showDialogDrawables(DialogListener dialogListener) {
        return showDialogDrawables((StyleProperty) null, dialogListener);
    }
    
    public DialogDrawables showDialogDrawables(DialogDrawablesListener listener, DialogListener dialogListener) {
        return showDialogDrawables((StyleProperty) null, listener, dialogListener);
    }
    
    public DialogDrawables showDialogDrawables(boolean allowSelection, DialogDrawablesListener listener, DialogListener dialogListener) {
        if (allowSelection) {
            return showDialogDrawables(new StyleProperty(), listener, dialogListener);
        } else {
            return showDialogDrawables(listener, dialogListener);
        }
    }

    public DialogFonts showDialogFonts(StyleProperty styleProperty, EventListener listener, DialogListener dialogListener) {
        DialogFonts dialog = new DialogFonts(styleProperty, listener);
        if (dialogListener != null) {
            dialog.addListener(dialogListener);
        }
        dialog.setFillParent(true);
        dialog.show(stage);
        dialog.refreshTable();
        return dialog;
    }

    public DialogFonts showDialogFonts(StyleProperty styleProperty, DialogListener dialogListener) {
        return showDialogFonts(styleProperty, null, dialogListener);
    }
    
    public DialogFonts showDialogFonts(CustomProperty customProperty, EventListener listener, DialogListener dialogListener) {
        DialogFonts dialog = new DialogFonts(customProperty, listener);
        if (dialogListener != null) {
            dialog.addListener(dialogListener);
        }
        dialog.setFillParent(true);
        dialog.show(stage);
        dialog.refreshTable();
        return dialog;
    }
    
    public DialogFonts showDialogFonts(CustomProperty customProperty, DialogListener dialogListener) {
        return showDialogFonts(customProperty, null, dialogListener);
    }

    public DialogFonts showFonts(DialogListener dialogListener) {
        return showDialogFonts((StyleProperty)null, dialogListener);
    }

    public void showDialogSettings(DialogListener dialogListener) {
        var pop = new PopSettings();
        if (dialogListener != null) {
            pop.addListener(dialogListener);
        }
        pop.show(stage);
    }

    public void showDialogColorPicker(DialogColorPicker.ColorListener listener) {
        showDialogColorPicker(null, listener);
    }

    public void showDialogColorPicker(Color previousColor,
            DialogColorPicker.ColorListener listener) {
        DialogColorPicker dialog = new DialogColorPicker("dialog", listener, previousColor);
        dialog.show(stage);
    }

    public void showNewStyleDialog(Skin skin, Stage stage) {
        Class selectedClass = rootTable.getSelectedClass();

        final TextField textField = new TextField("", skin);
        Dialog dialog = new Dialog("New Style", skin, "bg") {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    undoableManager.addUndoable(new NewStyleUndoable(selectedClass, textField.getText(), main), true);
                }
            }
        };
        dialog.getButtonTable().defaults().padBottom(10.0f).minWidth(50.0f);
        dialog.button("OK", true).button("Cancel", false);
        dialog.getButtonTable().getCells().first().getActor().addListener(handListener);
        dialog.getButtonTable().getCells().get(1).getActor().addListener(handListener);
        final TextButton okButton = (TextButton) dialog.getButtonTable().getCells().get(0).getActor();

        textField.setTextFieldListener((TextField textField1, char c) -> {
            if (c == '\n') {
                if (!okButton.isDisabled()) {
                    undoableManager.addUndoable(new NewStyleUndoable(selectedClass, textField1.getText(), main), true);
                    dialog.hide();
                }
                stage.setKeyboardFocus(textField1);
            }
        });

        textField.addListener(ibeamListener);

        dialog.getTitleLabel().setAlignment(Align.center);
        dialog.getContentTable().defaults().padLeft(10.0f).padRight(10.0f);
        dialog.text("What is the name of the new style?");
        dialog.getContentTable().getCells().first().pad(10.0f);
        dialog.getContentTable().row();
        dialog.getContentTable().add(textField).growX();
        okButton.setDisabled(true);

        Array<StyleData> currentStyles = projectData.getJsonData().getClassStyleMap().get(selectedClass);
        textField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                boolean disable = !StyleData.validate(textField.getText());

                if (!disable) {
                    for (StyleData data : currentStyles) {
                        if (data.name.equals(textField.getText())) {
                            disable = true;
                            break;
                        }
                    }
                }

                okButton.setDisabled(disable);
            }
        });

        dialog.key(Input.Keys.ESCAPE, false);

        dialog.show(stage);
        stage.setKeyboardFocus(textField);
        textField.setFocusTraversal(false);
    }

    public void showDuplicateStyleDialog(Skin skin, Stage stage) {
        Class selectedClass = rootTable.getSelectedClass();
        StyleData originalStyle = rootTable.getSelectedStyle();

        final TextField textField = new TextField("", skin);
        Dialog dialog = new Dialog("Duplicate Style", skin, "bg") {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    undoableManager.addUndoable(new DuplicateStyleUndoable(originalStyle, textField.getText(), main), true);
                }
            }
        };

        dialog.getButtonTable().defaults().padBottom(10.0f).minWidth(50.0f);
        dialog.button("OK", true).button("Cancel", false);
        final TextButton okButton = (TextButton) dialog.getButtonTable().getCells().get(0).getActor();
        okButton.addListener(handListener);
        dialog.getButtonTable().getCells().get(1).getActor().addListener(handListener);

        textField.setTextFieldListener((TextField textField1, char c) -> {
            if (c == '\n') {
                if (!okButton.isDisabled()) {
                    undoableManager.addUndoable(new DuplicateStyleUndoable(originalStyle, textField.getText(), main), true);
                    dialog.hide();
                }
                stage.setKeyboardFocus(textField1);
            }
        });

        textField.addListener(ibeamListener);

        dialog.getTitleLabel().setAlignment(Align.center);
        dialog.getContentTable().defaults().padLeft(10.0f).padRight(10.0f);
        dialog.text("What is the name of the new, duplicated style?");
        dialog.getContentTable().getCells().first().pad(10.0f);
        dialog.getContentTable().row();
        dialog.getContentTable().add(textField).growX();
        okButton.setDisabled(true);

        Array<StyleData> currentStyles = projectData.getJsonData().getClassStyleMap().get(selectedClass);
        textField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                boolean disable = !StyleData.validate(textField.getText());

                if (!disable) {
                    for (StyleData data : currentStyles) {
                        if (data.name.equals(textField.getText())) {
                            disable = true;
                            break;
                        }
                    }
                }

                okButton.setDisabled(disable);
            }
        });

        dialog.key(Input.Keys.ESCAPE, false);

        dialog.show(stage);
        stage.setKeyboardFocus(textField);

        textField.setFocusTraversal(false);
    }

    public void showDeleteStyleDialog(Skin skin, Stage stage) {
        StyleData styleData = rootTable.getSelectedStyle();

        Dialog dialog = new Dialog("Delete Style", skin, "bg") {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    undoableManager.addUndoable(new DeleteStyleUndoable(styleData, main), true);
                }
            }
        };
        dialog.getTitleLabel().setAlignment(Align.center);
        dialog.getContentTable().defaults().padLeft(10.0f).padRight(10.0f);
        dialog.text("Are you sure you want to delete style " + styleData.name + "?");
        dialog.getContentTable().getCells().first().pad(10.0f);

        dialog.getButtonTable().defaults().padBottom(10.0f).minWidth(50.0f);
        dialog.button("Yes, delete the style", true).button("No", false);
        dialog.getButtonTable().getCells().first().getActor().addListener(handListener);
        dialog.getButtonTable().getCells().get(1).getActor().addListener(handListener);

        dialog.key(Input.Keys.ENTER, true).key(Input.Keys.ESCAPE, false);

        dialog.show(stage);
    }

    public void showRenameStyleDialog(Skin skin, Stage stage) {
        Class selectedClass = rootTable.getSelectedClass();

        final TextField textField = new TextField(rootTable.getSelectedStyle().name, skin);
        Dialog dialog = new Dialog("Rename Style", skin, "bg") {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    undoableManager.addUndoable(new UndoableManager.RenameStyleUndoable(rootTable.getSelectedStyle(), main, textField.getText()), true);
                }
            }
        };
        dialog.getButtonTable().defaults().padBottom(10.0f).minWidth(50.0f);
        dialog.button("OK", true).button("Cancel", false);
        dialog.getButtonTable().getCells().first().getActor().addListener(handListener);
        dialog.getButtonTable().getCells().get(1).getActor().addListener(handListener);
        final TextButton okButton = (TextButton) dialog.getButtonTable().getCells().get(0).getActor();

        textField.setTextFieldListener((TextField textField1, char c) -> {
            if (c == '\n') {
                if (!okButton.isDisabled()) {
                    undoableManager.addUndoable(new UndoableManager.RenameStyleUndoable(rootTable.getSelectedStyle(), main, textField1.getText()), true);
                    dialog.hide();
                }
                stage.setKeyboardFocus(textField1);
            }
        });

        textField.addListener(ibeamListener);

        dialog.getTitleLabel().setAlignment(Align.center);
        dialog.getContentTable().defaults().padLeft(10.0f).padRight(10.0f);
        dialog.text("What would you like to rename the style \"" + rootTable.getSelectedStyle().name + "\" to?");
        dialog.getContentTable().getCells().first().pad(10.0f);
        dialog.getContentTable().row();
        dialog.getContentTable().add(textField).growX();
        okButton.setDisabled(true);

        Array<StyleData> currentStyles = projectData.getJsonData().getClassStyleMap().get(selectedClass);
        textField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                boolean disable = !StyleData.validate(textField.getText());

                if (!disable) {
                    for (StyleData data : currentStyles) {
                        if (data.name.equals(textField.getText())) {
                            disable = true;
                            break;
                        }
                    }
                }

                okButton.setDisabled(disable);
            }
        });

        dialog.key(Input.Keys.ESCAPE, false);

        dialog.show(stage);
        stage.setKeyboardFocus(textField);
        textField.selectAll();
        textField.setFocusTraversal(false);
    }
    
    public static interface CustomDrawableListener {
        public void run(String name);
    }
    
    public void showCustomDrawableDialog(Skin skin, Stage stage, CustomDrawableListener customDrawableListener) {
        showCustomDrawableDialog(skin, stage, null, customDrawableListener);
    }
    
    public void showCustomDrawableDialog(Skin skin, Stage stage, DrawableData modifyDrawable, CustomDrawableListener customDrawableListener) {
        final TextField textField = new TextField("", skin);
        Dialog dialog = new Dialog("New Custom Drawable", skin, "bg") {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    customDrawableListener.run(textField.getText());
                }
            }
        };
        dialog.getButtonTable().defaults().padBottom(10.0f).minWidth(50.0f);
        dialog.button("OK", true).button("Cancel", false);
        dialog.getButtonTable().getCells().first().getActor().addListener(handListener);
        dialog.getButtonTable().getCells().get(1).getActor().addListener(handListener);
        final TextButton okButton = (TextButton) dialog.getButtonTable().getCells().get(0).getActor();

        textField.setTextFieldListener((TextField textField1, char c) -> {
            if (c == '\n') {
                if (!okButton.isDisabled()) {
                    customDrawableListener.run(textField.getText());
                    
                    dialog.hide();
                }
                stage.setKeyboardFocus(textField1);
            }
        });

        textField.addListener(ibeamListener);
        if (modifyDrawable != null) {
            textField.setText(modifyDrawable.name);
        }

        dialog.getTitleLabel().setAlignment(Align.center);
        dialog.getContentTable().defaults().padLeft(10.0f).padRight(10.0f);
        if (modifyDrawable == null) {
            dialog.text("Enter the name for the custom drawable.\nUse to reference custom classes that inherit from Drawable.");
        } else {
            dialog.text("Enter the new name for the custom drawable.\nUse to reference custom classes that inherit from Drawable.");
        }
        ((Label)dialog.getContentTable().getCells().first().getActor()).setAlignment(Align.center);
        dialog.getContentTable().getCells().first().pad(10.0f);
        dialog.getContentTable().row();
        dialog.getContentTable().add(textField).growX();
        okButton.setDisabled(true);

        textField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                boolean disable = !StyleData.validate(textField.getText());

                if (atlasData.getDrawable(textField.getText()) != null) {
                    disable = true;
                }

                okButton.setDisabled(disable);
            }
        });

        dialog.key(Input.Keys.ESCAPE, false);

        dialog.show(stage);
        stage.setKeyboardFocus(textField);
        textField.selectAll();
        textField.setFocusTraversal(false);
    }
    
    public void showDrawableSettingsDialog(Skin skin, Stage stage, DrawableData drawable, DrawableSettingsListener listener) {
        var dialog = new Dialog("Drawable Settings", skin, "bg") {
            @Override
            protected void result(Object object) {
                if (listener != null) {
                    if ((Boolean) object) {
                        drawable.minWidth = ((Spinner) findActor("minWidth")).getValueAsInt();
                        drawable.minHeight = ((Spinner) findActor("minHeight")).getValueAsInt();
                        listener.result(true);
                    } else {
                        listener.result(false);
                    }
                }
            }
        };
        
        dialog.getTitleTable().pad(5);
        dialog.getContentTable().pad(5);
        dialog.getButtonTable().pad(5);
        
        var root = dialog.getContentTable();
        
        root.defaults().space(3);
        var label = new Label("Set values to -1 to disable.", skin);
        root.add(label).colspan(2);
        
        root.row();
        label = new Label("minWidth:", skin);
        root.add(label);
        
        var spinner = new Spinner(drawable.minWidth, 1, true, Spinner.Orientation.HORIZONTAL, skin);
        spinner.setMinimum(-1);
        spinner.setName("minWidth");
        root.add(spinner).width(100);
        spinner.getButtonMinus().addListener(handListener);
        spinner.getButtonPlus().addListener(handListener);
        spinner.getTextField().addListener(ibeamListener);
        
        root.row();
        label = new Label("minHeight:", skin);
        root.add(label);
        
        spinner = new Spinner(drawable.minHeight, 1, true, Spinner.Orientation.HORIZONTAL, skin);
        spinner.setMinimum(-1);
        spinner.setName("minHeight");
        root.add(spinner).width(100);
        spinner.getButtonMinus().addListener(handListener);
        spinner.getButtonPlus().addListener(handListener);
        spinner.getTextField().addListener(ibeamListener);
        
        var textButton = new TextButton("OK", skin);
        dialog.button(textButton, true);
        dialog.getButtonTable().add(textButton).uniform();
        textButton.addListener(handListener);
        
        textButton = new TextButton("Cancel", skin);
        dialog.button(textButton, false);
        textButton.addListener(handListener);
        
        dialog.show(stage);
    }
    
    public static interface DrawableSettingsListener {
        public void result(boolean accepted);
    }

    public Dialog showCloseDialog(DialogListener dialogListener) {
        if (projectData.areChangesSaved() || projectData.isNewProject()) {
            Gdx.app.exit();
        } else {
            if (!showingCloseDialog) {
                showingCloseDialog = true;
                Dialog dialog = new Dialog("Save Changes?", skin, "bg") {
                    @Override
                    public Dialog show(Stage stage, Action action) {
                        fire(new DialogEvent(DialogEvent.Type.OPEN));
                        return super.show(stage, action);
                    }
                    @Override
                    protected void result(Object object) {
                        if ((int) object == 0) {
                            mainListener.saveFile(() -> {
                                if (projectData.areChangesSaved()) {
                                    Gdx.app.postRunnable(() -> {
                                        Gdx.app.exit();
                                    });
                                }
                            });
                        } else if ((int) object == 1) {
                            Gdx.app.exit();
                        }

                        showingCloseDialog = false;
                        fire(new DialogEvent(DialogEvent.Type.CLOSE));
                    }
                };
                
                if (dialogListener != null) {
                    dialog.addListener(dialogListener);
                }
                
                dialog.getTitleTable().getCells().first().padLeft(5.0f);
                Label label = new Label("Do you want to save\nyour changes before you quit?", skin);
                label.setAlignment(Align.center);
                dialog.text(label);
                dialog.getContentTable().getCells().first().pad(10.0f);
                dialog.getButtonTable().defaults().padBottom(10.0f).minWidth(50.0f);
                dialog.button("Yes", 0).button("No", 1).button("Cancel", 2);
                dialog.getButtonTable().getCells().first().getActor().addListener(handListener);
                dialog.getButtonTable().getCells().get(1).getActor().addListener(handListener);
                dialog.getButtonTable().getCells().get(2).getActor().addListener(handListener);
                java.awt.Toolkit.getDefaultToolkit().beep();
                dialog.show(stage);
                return dialog;
            }
        }
        
        return null;
    }

    public void showDialogLoading(Runnable runnable) {
        DialogLoading dialog = new DialogLoading("", runnable, main);
        dialog.show(stage);
    }
    
    public void showSceneComposerDialog() {
        DialogSceneComposer dialog = new DialogSceneComposer();
        dialog.show(stage);
    }
    
    public Dialog showMessageDialog(String title, String text, DialogListener dialogListener) {
        Dialog dialog = new Dialog(title, skin, "bg") {
            @Override
            public Dialog show(Stage stage, Action action) {
                fire(new DialogEvent(DialogEvent.Type.OPEN));
                return super.show(stage, action);
            }
            @Override
            protected void result(Object object) {
                fire(new DialogEvent(DialogEvent.Type.CLOSE));
            }
        };
        
        if (dialogListener != null) {
            dialog.addListener(dialogListener);
        }
        
        dialog.getTitleTable().getCells().first().padLeft(5.0f);
        Label label = new Label(text, skin);
        label.setAlignment(Align.center);
        dialog.text(label);
        dialog.getContentTable().getCells().first().pad(10.0f);
        dialog.getButtonTable().defaults().padBottom(10.0f).minWidth(50.0f);
        dialog.button("OK");
        dialog.key(Input.Keys.ESCAPE, 0);
        dialog.key(Keys.ENTER, 0);
        dialog.key(Keys.SPACE, 0);
        dialog.show(stage);
        return dialog;
    }

    public Dialog yesNoDialog(String title, String text,
            ConfirmationListener listener, DialogListener dialogListener) {
        Dialog dialog = new Dialog(title, skin, "bg") {
            @Override
            public Dialog show(Stage stage, Action action) {
                fire(new DialogEvent(DialogEvent.Type.OPEN));
                return super.show(stage, action);
            }
            @Override
            protected void result(Object object) {
                listener.selected((int) object);
                fire(new DialogEvent(DialogEvent.Type.CLOSE));
            }
        };
        
        if (dialogListener != null) {
            dialog.addListener(dialogListener);
        }

        dialog.getTitleTable().getCells().first().padLeft(5.0f);
        Label label = new Label(text, skin);
        label.setAlignment(Align.center);
        dialog.text(label);
        dialog.getContentTable().getCells().first().pad(10.0f);
        dialog.getButtonTable().defaults().padBottom(10.0f).minWidth(50.0f);
        dialog.button("Yes", 0);
        dialog.button("No", 1);
        dialog.getButtonTable().getCells().first().getActor().addListener(handListener);
        dialog.getButtonTable().getCells().get(1).getActor().addListener(handListener);
        dialog.key(Input.Keys.ESCAPE, 1);
        dialog.key(Keys.ENTER, 0);
        dialog.key(Keys.SPACE, 0);
        dialog.show(stage);
        return dialog;
    }

    public Dialog yesNoCancelDialog(String title, String text,
            ConfirmationListener listener, DialogListener dialogListener) {
        Dialog dialog = new Dialog(title, skin, "bg") {
            @Override
            public Dialog show(Stage stage, Action action) {
                fire(new DialogEvent(DialogEvent.Type.OPEN));
                return super.show(stage, action);
            }
            @Override
            protected void result(Object object) {
                listener.selected((int) object);
                fire(new DialogEvent(DialogEvent.Type.CLOSE));
            }
        };
        
        if (dialogListener != null) {
            dialog.addListener(dialogListener);
        }


        dialog.getTitleTable().getCells().first().padLeft(5.0f);
        Label label = new Label(text, skin);
        label.setAlignment(Align.center);
        dialog.text(label);
        dialog.getContentTable().getCells().first().pad(10.0f);
        dialog.getButtonTable().defaults().padBottom(10.0f).minWidth(50.0f);
        dialog.button("Yes", 0);
        dialog.button("No", 1);
        dialog.button("Cancel", 2);
        dialog.getButtonTable().getCells().first().getActor().addListener(handListener);
        dialog.getButtonTable().getCells().get(1).getActor().addListener(handListener);
        dialog.getButtonTable().getCells().get(2).getActor().addListener(handListener);
        dialog.key(Input.Keys.ESCAPE, 2);
        dialog.key(Keys.ENTER, 0);
        dialog.key(Keys.SPACE, 0);
        dialog.show(stage);
        return dialog;
    }
    
    public void showInputDialog(String title, String message, String defaultText, InputDialogListener listener) {
        var dialog = new Dialog(title, skin, "bg") {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    listener.confirmed(((TextField) findActor("textField")).getText());
                } else {
                    listener.cancelled();
                }
            }
        };
        
        dialog.getTitleTable().getCells().first().padLeft(5.0f);
        
        var root = dialog.getContentTable();
        root.pad(5);
        
        root.defaults().space(5.0f);
        var label = new Label(message, skin);
        root.add(label);
        
        root.row();
        var textField = new TextField(defaultText, skin);
        textField.setName("textField");
        textField.setSelection(0, textField.getText().length());
        root.add(textField).growX();
        textField.addListener(ibeamListener);
        
        dialog.getButtonTable().pad(5);
        dialog.getButtonTable().defaults().space(5).minWidth(100);
        
        var button = new TextButton("OK", skin);
        dialog.button(button, true);
        button.addListener(handListener);
        
        button = new TextButton("Cancel", skin);
        dialog.button(button, false);
        button.addListener(handListener);
        
        dialog.key(Keys.ENTER, true).key(Keys.ESCAPE, false);
        
        dialog.show(stage);
        stage.setKeyboardFocus(textField);
    }
    
    public static interface InputDialogListener {
        public void confirmed(String text);
        public void cancelled();
    }

    public void showDialogError(String title, String message, Runnable runnable) {
        Dialog dialog = new Dialog(title, skin, "bg") {
            @Override
            public boolean remove() {
                if (runnable != null) {
                    runnable.run();
                }
                return super.remove();
            }

        };

        dialog.text(message);
        dialog.button("OK");
        dialog.getButtonTable().getCells().first().getActor().addListener(handListener);
        dialog.show(stage);
    }

    public void showDialogError(String title, String message) {
        DialogError dialog = new DialogError(title, message, main);
        dialog.show(stage);
    }

    public static void showDialogErrorStatic(String title, String message) {
        if (instance != null) {
            instance.showDialogError(title, message);
        }
    }

    public interface ConfirmationListener {

        public void selected(int selection);
    }

    public void showNewClassDialog(CustomClassListener listener) {
        DialogCustomClass dialog = new DialogCustomClass(main, "New Custom Class", false);
        dialog.addListener(listener);
        dialog.show(stage);
    }
    
    public void showRenameClassDialog(CustomClassListener listener) {
        Object selected = rootTable.getClassSelectBox().getSelected();
        if (selected instanceof CustomClass) {
            DialogCustomClass dialog = new DialogCustomClass(main, "Rename Custom Class", true, 
                    ((CustomClass) selected).getFullyQualifiedName(), 
                    ((CustomClass) selected).getDisplayName(), ((CustomClass) selected).isDeclareAfterUIclasses());
            dialog.addListener(listener);
            dialog.show(stage);
        }
    }
    
    public void showDuplicateClassDialog(CustomClassListener listener) {
        Object selected = rootTable.getClassSelectBox().getSelected();
        if (selected instanceof CustomClass) {
            DialogCustomClass dialog = new DialogCustomClass(main, "Duplicate Class", false,
                    ((CustomClass) selected).getFullyQualifiedName(), 
                    ((CustomClass) selected).getDisplayName(), ((CustomClass) selected).isDeclareAfterUIclasses());
            dialog.addListener(listener);
            dialog.show(stage);
        }
        
    }

    public void showNewCustomStyleDialog(CustomStyleListener listener) {
        DialogCustomStyle dialog = new DialogCustomStyle("New Style", false);
        dialog.addListener(listener);
        dialog.show(stage);
    }
    
    public void showDuplicateCustomStyleDialog(CustomStyleListener listener) {
        Object selected = rootTable.getStyleSelectBox().getSelected();
        if (selected instanceof CustomStyle) {
            DialogCustomStyle dialog = new DialogCustomStyle("Duplicate Style", false,
                    ((CustomStyle) selected).getName());
            dialog.addListener(listener);
            dialog.show(stage);
        }
    }
    
    public void showRenameCustomStyleDialog(CustomStyleListener listener) {
        Object selected = rootTable.getStyleSelectBox().getSelected();
        if (selected instanceof CustomStyle) {
            DialogCustomStyle dialog = new DialogCustomStyle("Rename Style", false,
                    ((CustomStyle) selected).getName());
            dialog.addListener(listener);
            dialog.show(stage);
        }
    }
    
    public void showNewStylePropertyDialog(CustomStylePropertyListener listener) {
        DialogCustomProperty dialog = new DialogCustomProperty("New Custom Property");
        dialog.addListener(listener);
        dialog.show(stage);
    }
    
    public void showRenameStylePropertyDialog(String propertyName, PropertyType propertyType, CustomStylePropertyListener listener) {
        DialogCustomProperty dialog = new DialogCustomProperty("Rename Custom Property", propertyName, propertyType, true);
        dialog.addListener(listener);
        dialog.show(stage);
    }
    
    public void showDuplicateStylePropertyDialog(String propertyName, PropertyType propertyType, CustomStylePropertyListener listener) {
        DialogCustomProperty dialog = new DialogCustomProperty("Duplicate Custom Property", propertyName, propertyType, false);
        dialog.addListener(listener);
        dialog.show(stage);
    }
    
    public void showDialogWelcome(WelcomeListener listener, DialogListener dialogListener) {
        var pop = new PopWelcome();
        
        if (dialogListener != null) {
            pop.addListener(dialogListener);
        }
        
        pop.addListener(listener);
        pop.show(stage);
    }
    
    public void showWarningDialog(Array<String> warnings) {
        DialogWarnings dialog = new DialogWarnings(main, true, warnings);
        dialog.show(stage);
    }
    
    public void showWarningDialog(boolean showCheckBox, Array<String> warnings) {
        DialogWarnings dialog = new DialogWarnings(main, showCheckBox, warnings);
        dialog.show(stage);
    }
    
    public void showDialogPathErrors(Array<DrawableData> drawableErrors, Array<FontData> fontErrors, Array<FreeTypeFontData> freeTypeFontErrors) {
        DialogPathErrors dialog = new DialogPathErrors(main, skin, "dialog", drawableErrors, fontErrors, freeTypeFontErrors);
        dialog.show(stage);
    }
    
    public void showDialogFreeTypeFont(DialogFreeTypeFontListener listener) {
        DialogFreeTypeFont dialog = new DialogFreeTypeFont();
        dialog.addListener(listener);
        dialog.setFillParent(true);
        dialog.show(stage);
        stage.setKeyboardFocus(dialog.findActor("fontName"));
        stage.setScrollFocus(dialog.findActor("scrollPane"));
    }
    
    public void showDialogFreeTypeFont(FreeTypeFontData data, DialogFreeTypeFontListener listener) {
        DialogFreeTypeFont dialog = new DialogFreeTypeFont(data);
        dialog.addListener(listener);
        dialog.setFillParent(true);
        dialog.show(stage);
        stage.setKeyboardFocus(dialog.findActor("fontName"));
        stage.setScrollFocus(dialog.findActor("scrollPane"));
    }
    
    public void showDialogImageFont(ImageFontListener imageFontListener) {
        DialogImageFont dialog = new DialogImageFont(imageFontListener);
        dialog.setFillParent(true);
        dialog.show(stage);
        stage.setKeyboardFocus(dialog.findActor("characters"));
        stage.setScrollFocus(dialog.findActor("scroll"));
    }
    
    public void showDialogBitmapFont(DialogBitmapFontListener listener) {
        DialogBitmapFont dialog = new DialogBitmapFont();
        dialog.addListener(listener);
        dialog.setFillParent(true);
        dialog.show(stage);
        stage.setScrollFocus(dialog.findActor("scrollPane"));
    }
    
    public void showDialogUpdate(Skin skin, Stage stage) {
        Dialog dialog = new Dialog("Download Update", skin, "bg");
        dialog.getContentTable().pad(10.0f);
        dialog.getButtonTable().pad(10.0f).padTop(0);
        
        Table table = dialog.getContentTable();
        
        Label label = new Label("Version " + Main.newVersion + " available for download.", skin);
        table.add(label);
        
        table.row();
        TextButton textButton = new TextButton("Download Here", skin, "link");
        table.add(textButton);
        textButton.addListener(handListener);
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                Gdx.net.openURI("https://github.com/raeleus/skin-composer/releases");
            }
        });
        
        dialog.getButtonTable().defaults().minWidth(80.0f);
        textButton = new TextButton("OK", skin);
        dialog.button(textButton);
        textButton.addListener(handListener);
        
        dialog.key(Keys.ENTER, true).key(Keys.ESCAPE, false);
        
        dialog.show(stage);
    }
    
    public void showDialog9Patch(ObjectMap<DrawableData, Drawable> drawablePairs, Dialog9PatchListener listener) {
        Dialog9Patch dialog = new Dialog9Patch(drawablePairs);
        dialog.addDialog9PatchListener(listener);
        dialog.setFillParent(true);
        dialog.show(stage);
    }
    
    public DialogCustomStyleSelection showDialogCustomStyleSelection(CustomProperty customProperty, DialogListener dialogListener) {
        var dialog = new DialogCustomStyleSelection();
        dialog.addListener(dialogListener);
        dialog.show(stage);
        dialog.addListener(new DialogCustomStyleSelection.DialogCustomStyleSelectionListener() {
            @Override
            public void confirmed(String style) {
                undoableManager.addUndoable(new UndoableManager.CustomStyleSelectionUndoable(main, customProperty, style), true);
            }

            @Override
            public void cancelled() {
                
            }
        });
        return dialog;
    }
    
    public void showDialogDrawablesFilter(DialogDrawables.FilterOptions filterOptions, EventListener listener) {
        var dialog = new DialogDrawablesFilter(filterOptions);
        dialog.addListener(listener);
        dialog.show(stage);
    }
    
    public void showDialogTenPatch(DrawableData drawableData, boolean newDrawable, DialogTenPatchListener listener) {
        DialogTenPatch dialog = new DialogTenPatch(drawableData, newDrawable);
        dialog.addListener(listener);
        dialog.show(stage);
    }
    
    public void showDuplicateDialog(String title, String message, String defaultText, InputDialogListener listener) {
        var dialog = new Dialog(title, skin, "bg") {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    listener.confirmed(((TextField) findActor("textField")).getText());
                } else {
                    listener.cancelled();
                }
            }
        };
        
        dialog.getTitleTable().getCells().first().padLeft(5.0f);
        
        var root = dialog.getContentTable();
        root.pad(5);
        
        root.defaults().space(5.0f);
        var label = new Label(message, skin);
        root.add(label);
        
        root.row();
        var textField = new TextField(defaultText, skin);
        textField.setName("textField");
        textField.setSelection(0, textField.getText().length());
        root.add(textField).growX();
        textField.addListener(ibeamListener);
        textField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                TextButton textButton = dialog.findActor("ok-button");
    
                boolean disable = !DrawableData.validate(textField.getText()) || (atlasData.checkIfNameExists(textField.getText()));
                textButton.setDisabled(disable);
            }
        });
        
        dialog.getButtonTable().pad(5);
        dialog.getButtonTable().defaults().space(5).minWidth(100);
        
        var button = new TextButton("OK", skin);
        button.setName("ok-button");
        button.setDisabled(true);
        dialog.button(button, true);
        button.addListener(handListener);
        
        button = new TextButton("Cancel", skin);
        dialog.button(button, false);
        button.addListener(handListener);
        
        dialog.key(Keys.ENTER, true).key(Keys.ESCAPE, false);
        
        dialog.show(stage);
        stage.setKeyboardFocus(textField);
    }
    
    public PopTable showToast(float duration, Skin skin, String style) {
        var popTable = new PopTable(skin, style);
        popTable.show(stage, Actions.fadeIn(.5f));
        popTable.addAction(Actions.sequence(Actions.delay(duration), Actions.run(() -> popTable.hide(Actions.fadeOut(.5f)))));
        
        return popTable;
    }
}
