/*******************************************************************************
 * Copyright 2019 Maximilian Stark | Dakror <mail@dakror.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.dakror.quarry.desktop;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.github.czyzby.lml.parser.impl.tag.Dtd;

import de.dakror.common.libgdx.GameBase.WindowMode;
import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import net.spookygames.gdx.sfx.desktop.DesktopAudioDurationResolver;

public class DesktopLauncher implements PlatformInterface {
    public static void main(String[] arg) {
        new DesktopLauncher(arg);
    }

    String[] arg;
    String version;

    JFrame errorFrame;

    public DesktopLauncher(String[] arg) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        DisplayMode dm = LwjglApplicationConfiguration.getDesktopDisplayMode();

        WindowMode mode = null;

        if (arg.length > 0 && (arg[0].equals("debug") || arg[0].equals("meta") || arg[0].equals("windowed"))) {
            if (dm.width == 1920) {
                config.width = 1280;
                config.height = 720;
            } else {
                config.width = 720;
                config.height = 405;
            }
            config.resizable = true;
        } else {
            config.width = dm.width;
            config.height = dm.height;
            mode = WindowMode.Borderless;
        }

        config.vSyncEnabled = true;
        config.audioDeviceSimultaneousSources = 32;

        /////////////////////

        int versionCode = 122;
        String version = "v122";

        /////////////////////

        this.version = version;
        this.arg = arg;

        config.addIcon("icon-16.png", FileType.Internal);
        config.addIcon("icon-32.png", FileType.Internal);
        config.addIcon("icon-64.png", FileType.Internal);

        config.title = "Drill Down";

        if (arg.length > 0 && arg[0].equals("textures")) {
            try {
                TexturePacker.main(new String[] { "./Development/Textures/", "./android/assets/", "tex.atlas",
                        "./android/assets/atlas-settings.json" });
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        DesktopAudioDurationResolver.initialize();
        Quarry game = new Quarry(this, true, versionCode, version, true, false, mode);
        new LwjglApplication(game, config);
    }

    @Override
    public Object message(int messageCode, Object payload) {
        switch (messageCode) {
            case PlatformInterface.MSG_EXCEPTION: {
                StringWriter sw = new StringWriter();
                ((Exception) payload).printStackTrace(new PrintWriter(sw));

                if (errorFrame == null) {
                    errorFrame = new JFrame("Error!");
                    errorFrame.setSize(500, 300);
                    errorFrame.setAlwaysOnTop(true);
                    errorFrame.setLocationRelativeTo(null);

                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    JLabel l = new JLabel("<html>" + Quarry.Q.i18n.get("ui.error") + "<br>Details:</html>");
                    l.setMaximumSize(new Dimension(450, 300));
                    l.setBorder(new EmptyBorder(20, 20, 20, 20));
                    l.setFont(l.getFont().deriveFont(14f));
                    panel.add(l, BorderLayout.NORTH);

                    JTextPane l1 = new JTextPane();
                    l1.setContentType("text/html");
                    l1.setText("<html><pre>" + sw.toString() + "</pre></html>");
                    l1.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
                    JScrollPane jsp = new JScrollPane(l1, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    jsp.setMaximumSize(new Dimension(450, 100));
                    panel.add(jsp, BorderLayout.CENTER);

                    panel.add(new JButton(new AbstractAction("Ok") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            errorFrame.setVisible(false);
                            errorFrame.dispose();
                            errorFrame = null;
                        }
                    }), BorderLayout.SOUTH);
                    errorFrame.setContentPane(panel);
                    errorFrame.setVisible(true);
                }

                ((Exception) payload).printStackTrace();
                break;
            }
            case Const.MSG_PADDING:
                return new int[4];
            case Const.MSG_DPI: {
                return Toolkit.getDefaultToolkit().getScreenResolution() / 96f;
            }
            case Const.MSG_PASTE: {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(payload.toString()),
                        null);
                break;
            }
            case Const.MSG_COPY: {
                try {
                    return Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                } catch (Exception e) {
                    return null;
                }
            }
            case -1230: {
                if (arg.length > 0 && arg[0].equals("meta") && version.equals("debug")) {
                    // META program
                    MetaGen.run();

                    try {
                        Writer writer = Gdx.files.absolute("../Development/lml.dtd").writer(false);
                        new Dtd().setDisplayLogs(false).getDtdSchema(((Quarry) payload).lml, writer);
                        writer.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }
                break;
            }
            case Const.MSG_FILE_PERMISSION: {
                return true;
            }
        }

        return null;
    }
}
