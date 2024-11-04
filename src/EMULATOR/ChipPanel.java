package EMULATOR;

import javax.swing.*;
import CHIP.Chip;
import java.awt.Color;
import java.awt.Graphics;

import java.awt.*;

public class ChipPanel extends JPanel {

    private Chip chip;

    public ChipPanel(Chip c) {
        this.chip = c;
    }

    public void paint(Graphics g) {
        byte[] display = chip.getDiplay();
        for(int i = 0; i < display.length; i++) {
            if(display[i] == 0){g.setColor(Color.black);}
            else{g.setColor(Color.white);}

            int xCoord = (i%64);
            int yCoord = (int) Math.floor(i/64);


            g.fillRect(xCoord*10, yCoord*10, 10, 10);
        }
    }

}
