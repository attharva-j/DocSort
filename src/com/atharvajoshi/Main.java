package com.atharvajoshi;

import java.io.IOException;
import java.nio.file.*;

public class Main {

    public static void main(String[] args) throws IOException{
	// write your code here
        String argList[] ={"C:/Users/Atharva Joshi"+"/Downloads/"};

        boolean recursive = false;
        int dirArg = 0;


        Path dir = Paths.get(argList[0]);
        new Categorize(dir,recursive).processEvents();

    }
}
