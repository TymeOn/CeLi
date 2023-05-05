#!/usr/bin/env python
#
# Copyright (c) Anthony, Inc. All rights reserved.
#
import glob
import os
import sys
import time
import Ice
import vlc

Ice.loadSlice('MusicPlayer.ice')
import SOAP

filePath = 'data/files'
uploadPath = 'data/uploads'


class ClientPlayer:
    def __init__(self, musicPlayer):
        self.matches = None
        self.vlcInstance = vlc.Instance()
        self.player = self.vlcInstance.media_player_new()
        self.player.set_mrl('rtsp://127.0.0.1:4000/lowSpotify')
        self.musicFileList = musicPlayer.searchMusic('')

    def pause(self):
        self.player.pause()

    def play(self):
        self.player.play()

    def stop(self):
        self.player.stop()

    def complete(self, text, state):
        if state == 0:
            if text:
                self.matches = [s for s in self.musicFileList
                                if s and s.startswith(text)]
            else: 
                self.matches = self.musicFileList[:]

        try:
            return self.matches[state]
        except IndexError:
            return None


def run(communicator):
    musicPlayer = SOAP.MusicPlayerPrx.checkedCast(
        communicator.propertyToProxy('MusicPlayer.Proxy').ice_twoway().ice_secure(True))
    if not musicPlayer:
        print('Invalid Proxy')
        sys.exit(1)

    clientPlayer = ClientPlayer(musicPlayer)

    menu()

    c = None
    while c != 'quit':
        try:
            sys.stdout.write('==> ')
            sys.stdout.flush()
            c = sys.stdin.readline().strip()
            # Permet d'arrêter un fichier MP3 avec VLC
            if c == 'stop':
                clientPlayer.stop()
                musicPlayer.stopMusic()
                menu()
            # Permet de mettre pause un fichier MP3 avec VLC
            elif c == 'pause':
                clientPlayer.pause()
                menu()
            # Permet de lancer un fichier MP3 avec VLC
            elif c == 'play':
                print('Liste des fichiers pouvant être écouté :')
                sourceDir = os.getcwd()
                os.chdir(uploadPath)

                if len(os.listdir(sourceDir + '/' + uploadPath)) == 0:
                    print("Aucun fichier upload")
                    os.chdir(sourceDir)
                    menu()
                    continue

                for musics in glob.glob('*.mp3'):
                    print(musics.replace('.mp3', ''))

                print('Saisissez le nom de la musique que vous voulez lancer :')
                musicName = input()
                result = musicPlayer.playMusic(musicName)

                if result:
                    clientPlayer.play()

                time.sleep(0.5)
                os.chdir(sourceDir)
                menu()
            # Permet de reprendre un fichier MP3 avec VLC
            elif c == 'resume':
                clientPlayer.play()
                menu()
            # Permet de déplacer un fichier MP3 dans un autre dossier
            elif c == 'upload':
                print('Liste des fichiers à upload :')
                sourceDir = os.getcwd()

                os.chdir(filePath)

                for musics in glob.glob('*.mp3'):
                    print(musics.replace('.mp3', ''))

                print('Saisissez le nom du fichier que vous voulez upload')
                musicName = input()
                musicMp3 = musicName + '.mp3'

                if not os.path.exists(musicMp3):
                    print('Aucun fichier trouvé')
                    os.chdir(sourceDir)
                    menu()
                    continue

                os.chdir(sourceDir)
                os.chdir(uploadPath)

                if os.path.exists(musicMp3):
                    print('Fichier déja upload')
                    os.chdir(sourceDir)
                    menu()
                    continue

                os.chdir(sourceDir)
                os.chdir(filePath)

                musicFile = open(musicMp3, 'rb')
                musicFileSize = os.stat(musicMp3).st_size
                quotients, remains = divmod(musicFileSize, 300000)

                number = musicPlayer.getNumber()

                for i in range(quotients):
                    partition = musicFile.read(300000)
                    musicPlayer.uploadPartition(number, partition)

                partition = musicFile.read(remains)
                musicPlayer.uploadPartition(number, partition)

                musicFile.close()

                musicPlayer.uploadMusic(number, musicMp3)
                os.chdir(sourceDir)

                print('Fichier upload avec succés')

                menu()
            # Permet de supprimer un fichier MP3 qui est dans le dossier spécifique
            elif c == 'delete':
                sourceDir = os.getcwd()
                os.chdir(uploadPath)

                for musics in glob.glob('*.mp3'):
                    print(musics.replace('.mp3', ''))

                os.chdir(sourceDir)

                print('Saisissez le nom de la musique que vous voulez supprimer')
                musicName = input('')

                result = musicPlayer.deleteMusic(musicName)

                if result:
                    print('Fichier supprimé avec succès')
                else:
                    print('Aucun fichier trouvé')

                menu()
            # Permet de renommer un fichier MP3 qui est dans le dossier spécifique
            elif c == 'rename':
                sourceDir = os.getcwd()
                os.chdir(uploadPath)

                for musics in glob.glob('*.mp3'):
                    print(musics.replace('.mp3', ''))

                os.chdir(sourceDir)
                print('Saisissez le nom de la musique que vous voulez renommer')
                oldMusicName = input()

                print('Saisissez le nouveau nom de la musique')
                newMusicName = input('')

                result = musicPlayer.renameMusic(oldMusicName, newMusicName)

                if result:
                    print('Fichier renommé avec succès')
                else:
                    print('Aucun fichier trouvé')

                menu()
            # Permet de rechercher un fichier MP3 qui est dans le dossier spécifique
            elif c == 'search':
                print('Saisissez le nom de la musique ou de l\'auteur que vous voulez chercher :')
                musicName = input()
                clientPlayer.musicFileList = musicPlayer.searchMusic(musicName)

                if not clientPlayer.musicFileList: print('Aucun fichier trouvé')

                print('Voici la liste des musiques trouvées :')
                for music in clientPlayer.musicFileList:
                    print('- ' + music)

                menu()
            elif c == 'quit':
                pass  # Nothing to do

            elif c == 'help':
                menu()

            else:
                print('Commande inconnue `' + c + '\'')
                menu()

        except Ice.Exception as ex:
            print(ex)


def menu():
    print('''
    usage:
        stop    : Arrêter la musique
        pause   : Mettre la musique en pause
        play    : Lancer une musique de votre choix
        resume  : Reprendre la musique 
        upload  : Upload une musique de votre choix
        delete  : Supprimer une musique de votre choix
        rename  : Renommer une musique de votre choix
        search  : Trouver toutes les musiques disponibles contenant une chaîne de caractères spécifiques
        help    : Affichage du menu
        quit    : Quitter le programme
    ''')


#
# Ice.initialize returns an initialized Ice communicator,
# the communicator is destroyed once it goes out of scope.
#
with Ice.initialize(sys.argv, 'config.client') as communicator:
    #
    # The communicator initialization removes all Ice-related arguments from argv
    #
    if len(sys.argv) > 1:
        print(sys.argv[0] + ': trop d\'arguments')
        sys.exit(1)

    run(communicator)
