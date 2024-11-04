package CHIP;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

public class Chip {
    //The chip has 8 bytes of memory and I will be using an array of characters to represent it.
    private char[] chipMemory;
    private char[] v; //The Chip8 has 16 register and it is typically referred to as v, so I will name my array as such for practicality.
    private char I; //Address pointer call I, 16 bit wide (but really only need 12).
    private char programCounter;
    private char stack[];
    private int stackPointer;

    private int delay_timer;
    private int sound_timer;

    private byte[] keys; //Hexadecimal keyboard

    private byte[] display; // 0 for black. 1 for white.

    private boolean needRedraw;

    public Chip() {
        this.chipMemory = new char[4096]; //4096 slots for the memory the chip needs.
        this.v = new char[16];
        this.I = 0x0;
        this.programCounter = 0x200;
        this.stack = new char[16];
        this.stackPointer = 0;
        this.delay_timer = 0;
        this.sound_timer = 0;
        this.keys = new byte[16];
        this.display = new byte[64*32];

        this.needRedraw = false;
        loadFontSet();
    }

    public void run(){
        //Fetch operation code. Decode Opcode. Execute opcode.
        char operationCode = (char) ((this.chipMemory[this.programCounter] << 8) | this.chipMemory[this.programCounter+1]);
        switch(operationCode & 0xF00) /* Grab all first values from the operation code.*/{

            case 0x0000:{
                switch (operationCode & 0x000FF){
                    case 0x00E0:{
                        break;
                    }
                    case 0x00EE:{
                        this.stackPointer--;
                        this.programCounter = (char)(this.stack[stackPointer] + 2);
                        System.out.println("Returning to program counter.");
                        break;
                    }
                    default:{
                        break;
                    }
                }
                 break;
            }

            case 0x1000: {//Jumps to address NNN
                int nnn = operationCode & 0X0FFF;
                this.programCounter = (char) nnn;
                System.out.println("Jumping");
                break;
            }
            case 0x2000: {//Calls subroutine at NNN
                stack[stackPointer] = this.programCounter;
                this.stackPointer++;
                this.programCounter = (char) (operationCode & 0x0FFF);
                break;
            }
            case 0x3000: {//Skips the next instruction if VX equals NN
                int x = (operationCode & 0x0F00) >> 8;
                int nn = (operationCode & 0x0FFF);
                if (this.v[x] == nn) {
                    this.programCounter += 4;
                    System.out.println("Skipping next instruction.");
                } else {
                    this.programCounter += 2;
                    System.out.println("Not skipping the next instruction.");
                }

                break;
            }

            case 0x4000: { //4XNN skip the next instruction if VX != nn
                int x = (operationCode & 0x0F00) >> 8;
                int nn = (operationCode & 0x0FFF);
                if(x != nn){
                    this.programCounter += 4;
                }
                else{
                    this.programCounter += 2;
                }
                System.out.println("Skipping next instruction.");
                break;
            }

            case 0x6000: //Set VC to NN
                int index = (operationCode & 0x0F00) >> 8;
                this.v[index] = (char)(operationCode & 0x00FF);
                this.programCounter += 2;
                System.out.println("Setting V[" + index + "] to " + (int) this.v[index]);
                break;


            case 0x7000: //Adds NN to VX
                int _index = (operationCode & 0x0F00) >> 8;
                int _n = (operationCode & 0x00FF);
                this.v[_index] = (char)(this.v[_index] + _n & 0xFF);
                this.programCounter += 2;
                break;


            case 0x8000:

                switch (operationCode & 0x000F){

                    case 0x0002:{ //Sets VX TO VY and VY
                        int x = (operationCode & 0x0F00) >> 8;
                        int y = (operationCode & 0x00F0) >> 4;
                        this.v[x] = (char)(this.v[x] & this.v[y]);
                        this.programCounter += 2;
                        System.out.println("Setting V[" + x + "] to " + (int) this.v[x]);
                        break;
                    }

                    case 0x0004: { //Adds VY TO VX. VF is set to 1 if carry applies
                        int x = (operationCode & 0x0F00) >> 8;
                        int y = (operationCode & 0x00F0) >> 4;
                        System.out.println("Setting V[" + x + "]");
                        if(this.v[y] > 255 - this.v[x]){
                            this.v[0xF] = 1;
                        }
                        else{
                            this.v[0xF] = 0;
                        }
                        this.v[x] = (char)((this.v[x] + this.v[y]) & 0xFF);
                        this.programCounter += 2;
                        break;
                    }

                        default:
                            System.err.println("Unsupported operation code.");
                            System.exit(0);
                            break;
                }
                break;
            case 0xA000: {//ANNN Set I to NNN
                this.I = (char) (operationCode & 0x0FFF);
                this.programCounter += 2;
                break;
            }

            case 0xC000: { //CXNN set VX to a random number NN
                int x = (operationCode & 0x0F00) >> 8;
                int nn = (operationCode & 0x00FF);
                int randomNumber = new Random().nextInt(256) & nn;
                this.v[x] = (char) randomNumber;
                System.out.println("VX has been set to a randomized number.");
                this.programCounter += 2;
                break;
            }

            case 0xD000: { //DXYN draw a sprite
                int x2 = this.v[(operationCode & 0x0F00) >> 8];
                int y2 = this.v[(operationCode & 0x00FF) >> 4];
                int height = operationCode & 0x0F00;

                this.v[0xF] = 0;

                for (int y = 0; y < height; y++) {
                    int line = this.chipMemory[this.I + y];
                    for (int x = 0; x < x2; x++) {
                        int pixel = line & (0x80 >> x);
                        if (pixel != 0) {
                            int totalX = x2 + x;
                            int totalY = y2 + y;
                            int index2 = totalY * 64 + totalX;

                            if (this.display[index2] == 1) {
                                this.v[0xF] = 1;
                            }
                            this.display[index2] ^= 1;
                        }
                    }
                }
                this.programCounter += 2;
                needRedraw = true;
                System.out.println("Drawing at the designated position.");
                break;
            }

            case 0xE000:{
                switch (operationCode & 0x00FF){
                    case 0x009E:{ //EX9E skip the next instruction if the key X is pressed.
                        int x = (operationCode & 0x0F00) >> 8;
                        int key = this.v[x];
                        if(this.keys[key] == 1){this.programCounter+=4;}
                        else{this.programCounter+=2;}
                        break;
                    }
                    case 0x00A1:{ //EXA1 skip the next instruction if the key VX is NOT pressed.
                        int x = (operationCode & 0x0F00) >> 8;
                        int key = this.v[x];
                        if(this.keys[key] == 0){this.programCounter+=4;}
                        else{this.programCounter+=2;}
                        break;
                    }
                    default:{
                        System.out.println("Unsupported operation code.");
                        break;
                    }
                }
                break;
            }

            case 0xF000:{
                switch (operationCode & 0x00FF){

                    case 0x0007:{ //Set VX to the value of the delay timer.
                        int x = (operationCode & 0x0F00) >> 8;
                        this.v[x] = (char) this.delay_timer;
                        this.programCounter += 2;
                        System.out.println("Has been set to delay timer.");

                    }

                    case 0x0015:{ //Set delayer time
                        int x = (operationCode & 0x0F00) >> 8;
                        this.delay_timer = this.v[x];
                        this.programCounter+=2;
                        System.out.println("Setting delay timer.");
                    }

                    case 0x0029:{ // Sets I to the location of the sprite for the character VX.
                        int x = (operationCode & 0x0F00) >> 8;
                        int character = this.v[x];
                        this.I = (char)(0x050 + (character * 5));
                        System.out.println("Setting I to Character offset.");
                        this.programCounter+=2;
                        break;
                    }

                    case 0x0033:{ //Storing binary-coded decimal value VX in I
                        int x = (operationCode & 0x0F00) >> 8;
                        int value = this.v[x];
                        int hundreds = (value - (value % 100))/100;
                        value-=hundreds * 100;
                        int tens = (value - (value % 10))/10;
                        value-=tens * 10;
                        this.chipMemory[this.I] = (char)hundreds;
                        this.chipMemory[this.I + 1] = (char)tens;
                        this.chipMemory[this.I + 2] = (char)value;
                        System.out.println("Storing binary coded deicmal.");
                        this.programCounter+=2;
                        break;
                    }
                    case 0x0065:{ // FX65 fills V0 to VX with values from I
                        int x  = (operationCode & 0x0F00) >> 8;
                        for(int i = 0; i < x; i++){
                            this.v[i] = this.chipMemory[this.I + i];
                        }
                        this.I = (char)(I+x+1);
                        System.out.println("Setting V[0] to the values of memory.");
                        this.programCounter += 2;
                        break;
                    }
                    default:{
                        break;
                    }
                }
                break;
            }

            default:
                System.err.println("Unsupported operation code.");
                System.exit(0);
        }


    }

    public byte[] getDiplay() {
        return this.display;
    }

    public boolean needsRedraw(){
        return this.needRedraw;
    }

    public void removeDrawFlag(){
        this.needRedraw = false;
    }

    public void loadProgram(String file){
        DataInputStream input = null;
        try{
            input = new DataInputStream(new FileInputStream(new File(file)));
            int offset = 0;
            while(input.available() > 0){
                this.chipMemory[offset + 0x200] = (char) (input.readByte() & 0xFF);
                offset++;
            }
        }catch (IOException e){
            e.printStackTrace();
            System.exit(0);
        }finally {
            if(input != null){
                try{
                    input.close();
                }catch (IOException e){}
            }
        }
    }

    public void loadFontSet(){
        for(int i = 0; i < ChipData.fontset.length; i++){
            this.chipMemory[0x50 + i] = (char)(ChipData.fontset[i] & 0xFF);
        }
    }

    public void setKeyBuffer(int[] keyBuffer){
        for(int i = 0; i < keys.length; i++){
            this.keys[i] = (byte)keyBuffer[i];
        }
    }
}
