package org.networks.java.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;

class FileDownloadManagerTest {

    @Test
    void addFilePiece() throws IOException {
        Random rd = new Random();
        byte[] arr = new byte[100];
        rd.nextBytes(arr);
//        FileDownloadManager handler = new FileDownloadManager("E:\\Courses\\Semester 3\\CN\\Project\\p2p-torrent\\peer1002", true, new CommonConfig(), "1002");
//        handler.addFilePiece(arr, 0, "1002");
    }

    @Test
    void getFilePiece() {
    }

    @Test
    void completeScenario() throws IOException {
        Random rd = new Random();
        byte[] arr = new byte[100];
        rd.nextBytes(arr);

        CommonConfig commonConfig = new CommonConfig("E:\\Courses\\Semester 3\\CN\\Project\\p2p-torrent\\Common.cfg");

//        FileDownloadManager handler = new FileDownloadManager("E:\\Courses\\Semester 3\\CN\\Project\\p2p-torrent\\peer1002", true, commonConfig, "1002");
        for (int i = 0; i < 5; i++) {
            arr = new byte[100];
            rd.nextBytes(arr);
//            handler.addFilePiece(arr, i, "1002");
//            byte[] filePiece = handler.getFilePiece(i);
//            Assertions.assertArrayEquals(arr, filePiece);
        }
//        handler.writeFileToDisk();
    }
}