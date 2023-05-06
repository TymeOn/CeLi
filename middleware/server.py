#!/usr/bin/env python
#
# Copyright (c) Anthony, Inc. All rights reserved.
#

import signal
import sys
import Ice
import os
import glob
import vlc

Ice.loadSlice('MusicPlayer.ice')
import SOAP

uploadPath = 'data/uploads'


def fileExists(filePath):
    if os.path.exists(filePath):
        return True
    else:
        print('Fichier introuvable')
        return False


class MusicPlayerI(SOAP.MusicPlayer):

    def __init__(self):
        self.player = vlc.Instance()
        self.mediaPlayer = self.player.media_player_new()
        self.number = 0
        self.musicFileList = {}

    # Permet de renommer un fichier MP3 qui est dans le dossier spécifique
    def renameMusic(self, filename, newFilename, current):
        uploadFilePath = uploadPath + '/' + filename + '.mp3'
        uploadNewFilePath = uploadPath + '/' + newFilename + '.mp3'

        if not fileExists(uploadFilePath):
            return False

        os.rename(uploadFilePath, uploadNewFilePath)
        print('Fichier renommé avec succès')

        return True

    # Permet de déplacer un fichier MP3 dans un autre dossier
    def uploadMusic(self, number, filename, current):
        file = open(uploadPath + '/' + filename, 'wb')
        file.write(self.musicFileList[number])
        file.close()
        print('Fichier upload avec succés')
        return True
        
    # Permet de supprimer un fichier MP3 qui est dans le dossier spécifique
    def deleteMusic(self, filename, current):
        uploadFilePath = uploadPath + '/' + filename + '.mp3'

        if not fileExists(uploadFilePath):
            return False

        os.remove(uploadFilePath)
        print('Fichier supprimé avec succès')

        return True

    # Permet d'arrêter un fichier MP3 avec VLC
    def stopMusic(self, current):
        self.mediaPlayer.stop()

    # Permet de rechercher un fichier MP3 qui est dans le dossier spécifique
    def searchMusic(self, filename, current):

        sourceDir = os.getcwd()
        os.chdir(uploadPath)
        count = 0

        fileList = []

        for musicFile in glob.glob('*' + filename + '*.mp3'):
            count += 1
            fileList.append(musicFile.rsplit('.mp3', 1)[0])

        os.chdir(sourceDir)

        return fileList

    # Permet de partitionner un fichier MP3
    def uploadPartition(self, number, partition, current):
        if number not in self.musicFileList:
            self.musicFileList[number] = b''

        self.musicFileList[number] += partition

        return True

    def getNumber(self, current):
        number = self.number
        self.number += 1
        return number

    # Permet de lancer un fichier MP3 avec VLC
    def playMusic(self, filename, current):
        print(filename)
        musicFile = uploadPath + '/' + filename + '.mp3'

        if not fileExists(musicFile):
            return False

        media = self.player.media_new(musicFile)

        media.add_option('sout=#rtp{mux=ts,ttl=10,port=7000,sdp=rtsp://192.168.200.238:7000/lowSpotify}')
        media.add_option('--sout-keep')
        media.add_option('--no-sout-all')
        media.get_mrl()

        self.mediaPlayer = self.player.media_player_new()
        self.mediaPlayer.set_media(media)
        self.mediaPlayer.play()

        return True


# Ice.initialize returns an initialized Ice communicator,
# the communicator is destroyed once it goes out of scope.

with Ice.initialize(sys.argv, 'config.server') as communicator:
    #
    # Install a signal handler to shutdown the communicator on Ctrl-C
    #
    signal.signal(signal.SIGINT, lambda signum, frame: communicator.shutdown())

    #
    # The communicator initialization removes all Ice-related arguments from argv
    #
    if len(sys.argv) > 1:
        print(sys.argv[0] + ': trop d\'arguments')
        sys.exit(1)

    adapter = communicator.createObjectAdapter('MusicPlayer')
    adapter.add(MusicPlayerI(), Ice.stringToIdentity('musicplayer'))
    adapter.activate()
    communicator.waitForShutdown()
