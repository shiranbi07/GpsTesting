package com.example.samplelocation;

import java.util.List;

public class FreeParkings {

    static class FreeParking {

        public String name;
        public int count;

        public  FreeParking(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    List<FreeParking> freeParkinglist;
    FreeParkings(List<FreeParking> freeParkinglist) {
        this.freeParkinglist = freeParkinglist;
    }
}
