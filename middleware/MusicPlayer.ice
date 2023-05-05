//
// Copyright (c) ZeroC, Inc. All rights reserved.
//

#pragma once

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
