package com.atharvajoshi;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

public class Categorize {
    private final WatchService WATCHER;
    private final Map<WatchKey,Path> keys;
    private final boolean recursive;
    private boolean trace = false;

    public static String audioFileExt[] = {"mp3","wav","wv","m4a","awb","aa","3gp"};
    public static String docFileExt[] = {"rtf","doc","doc","docx","tex","txt","odt","pdf"};
    public static String vidFileExt[] = {"flv","vob","ogg","ogv","mp4","gif","avi"};
    public static String imgFileExt[] = {"jpeg","jpeg","tmp","exif","tiff","png","bmp","bat"};

    public Categorize(Path dir, boolean recursive) throws IOException { //creates a watchservice and registers the given directory
        this.WATCHER = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.recursive = recursive;

        if (recursive){
            System.out.format("Scanning %s ...\n", dir);
            registerAll(dir);
            System.out.println("Done.");
        }else {
            register(dir);
        }

        this.trace = true;
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event){
        return (WatchEvent<T>) event;
    }

    private void register (Path dir) throws IOException{

        WatchKey key = dir.register(WATCHER, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace){
            Path prev = keys.get(key);
            if (prev == null){
                System.out.format("register: %s\n", dir);
            }else{
                if (!dir.equals(prev)){
                    System.out.format("update %s -> %sn\n",prev, dir);
                }
            }
        }
        keys.put(key,dir);
    }

    private void registerAll(final Path start) throws IOException{
        Files.walkFileTree(start, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return null;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return null;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return null;
            }
        });
    }
    private static String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".")+1);
        else return "";
    }

    public static WatchEvent.Kind kind;
    public static Path name;

    void processEvents(){
        while(true){
            WatchKey key;
            try{
                key = WATCHER.take();
            }catch (InterruptedException x){return;}

            Path dir = keys.get(key);
            if(dir == null){
                System.err.println("WatchKey not recognized!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()){
                kind = event.kind();

                if (kind == OVERFLOW){
                    continue;
                }

                WatchEvent<Path> ev = cast(event);
                name = ev.context();
                Path child = dir.resolve(name);

                // print out event
                System.out.format("%s: %s\n", event.kind().name(), child);

                if (recursive && (kind == ENTRY_CREATE)) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readable
                    }
                }
            }

            if(kind == ENTRY_CREATE){
                File file = new File(dir.toString() + "/"+name.toString());
                String ext = getFileExtension(file);
                System.out.println(ext);

                for(int i=0;i<audioFileExt.length;i++)
                {
                    if(ext.equals(audioFileExt[i]))
                    {
                        Path src = FileSystems.getDefault().getPath(dir.toString() +"/"+ name.toString());
                        Path dest = FileSystems.getDefault().getPath(dir.toString() + "/Audio/"+name.toString());
                        try{Files.move(src,dest,REPLACE_EXISTING);}catch(IOException e){e.printStackTrace();}
                    }
                }

                for(int i=0;i<docFileExt.length;i++)
                {
                    if(ext.equals(docFileExt[i]))
                    {
                        System.out.println(ext);
                        Path src = FileSystems.getDefault().getPath(dir.toString() +"/"+ name.toString());
                        Path dest = FileSystems.getDefault().getPath(dir.toString() + "/Documents/"+name.toString());
                        try{Path temp = Files.move(src,dest,REPLACE_EXISTING); if(temp != null) {
                            System.out.println("Moved!");
                        }else {
                            System.out.println("FAILED");
                        }}catch(IOException e){e.printStackTrace();}
                    }
                }

                for(int i=0;i<imgFileExt.length;i++)
                {
                    if(ext.equals(imgFileExt[i]))
                    {
                        System.out.println(ext);
                        Path src = FileSystems.getDefault().getPath(dir.toString() +"/"+ name.toString());
                        Path dest = FileSystems.getDefault().getPath(dir.toString() + "/Images/"+name.toString());
                        try{Files.move(src,dest,REPLACE_EXISTING);}catch(IOException e){e.printStackTrace();}
                    }
                }
            }

            boolean valid = key.reset();

            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }
}
