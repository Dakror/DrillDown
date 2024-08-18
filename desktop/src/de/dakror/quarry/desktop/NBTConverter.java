/*******************************************************************************
 * Copyright 2018 Maximilian Stark | Dakror <mail@dakror.de>
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

import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import de.dakror.common.libgdx.io.NBT;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.CompressionType;

/**
 * @author Maximilian Stark | Dakror
 */
public class NBTConverter extends JFrame {

    private static final long serialVersionUID = 1L;

    public NBTConverter() {
        super("NBTConverter");

        JPanel p = new JPanel();
        p.add(new JButton(new AbstractAction("BIN to TXT") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser(new File("C:\\Users\\Dakror\\TheQuarry\\saves"));
                jfc.setMultiSelectionEnabled(false);
                jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (jfc.showOpenDialog(NBTConverter.this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        CompoundTag t = NBT.read(new BufferedInputStream(new FileInputStream(jfc.getSelectedFile())), CompressionType.Fast);

                        if (jfc.showSaveDialog(NBTConverter.this) == JFileChooser.APPROVE_OPTION) {
                            BufferedWriter bw = new BufferedWriter(new FileWriter(jfc.getSelectedFile()));
                            bw.write(t.toString());
                            bw.close();

                            JOptionPane.showMessageDialog(NBTConverter.this, jfc.getSelectedFile().getPath() + " saved.");
                        }
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }));
        p.add(new JButton(new AbstractAction("TXT to BIN") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser(new File("C:\\Users\\Dakror\\TheQuarry\\saves"));
                jfc.setMultiSelectionEnabled(false);
                jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (jfc.showOpenDialog(NBTConverter.this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        CompoundTag t = NBT.readText(new FileInputStream(jfc.getSelectedFile()));

                        if (jfc.showSaveDialog(NBTConverter.this) == JFileChooser.APPROVE_OPTION) {
                            NBT.write(new FileOutputStream(jfc.getSelectedFile()), t, CompressionType.Fast);

                            JOptionPane.showMessageDialog(NBTConverter.this, jfc.getSelectedFile().getPath() + " saved.");
                        }
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }));
        setContentPane(p);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        new NBTConverter();
    }

}
