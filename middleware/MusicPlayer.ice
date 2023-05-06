//
// Copyright (c) ZeroC, Inc. All rights reserved.
//

#pragma once
["java:package:com.anmvg.celi"]
module SOAP
{
    sequence<string> list;
    sequence<byte> seq;

    interface MusicPlayer
    {
        bool renameMusic(string filename, string newFilename);
        bool deleteMusic(string filename);
        list searchMusic(string filename);
        bool playMusic(string filename);
        void stopMusic();
        bool uploadPartition(int number, seq partition);
        bool uploadMusic(int number, string filename);
        int getNumber();
    }
}
