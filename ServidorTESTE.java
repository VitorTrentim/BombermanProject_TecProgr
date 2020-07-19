/*
Trabalho de Técnicas de Programaçao - BSI UNESP Bauru
Ettore Scolar e Vitor Trentim
*/

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.*;
import javax.swing.border.Border;

//import jdk.internal.org.jline.utils.InputStreamReader;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Scanner;

class ServidorTESTE extends Thread {
    //// DADOS DE CONEXÃO
    int PORTO = 12345;
    ServerSocket serverSocket = null;
    boolean boolQuantPlayersRecebida = false, boolIniciaJogo = false;
    Socket arrayPlayerSockets[] = new Socket[2];

    //// DADOS PLAYER
    final int PERS1 = 1, PERS2 = 2;
    PlayerThread arrayPlayerThread[] = new PlayerThread[2];
    String stringQuantidadeDePlayers;
    int intQuantidadeDePlayers;

    final int PARADO = 0, ANDANDO_DIREITA = 1, ANDANDO_ESQUERDA = 2, ANDANDO_FRENTE = 3, ANDANDO_COSTAS = 4,
            DANIFICADO = 5, LENGTH_IMAGENS_PLAYER = 6;
    String nome_do_Player, score_do_Player = null;

    //// DADOS INIMIGO
    String morcegoDireita = "morcegoDireita", morcegoEsquerda = "morcegoEsquerda", morcegoCima = "morcegoCima",
            morcegoBaixo = "morcegoBaixo";
    String cavaleiroBaixo = "cavaleiroBaixo", cavaleiroCima = "cavaleiroCima", cavaleiroDireita = "cavaleiroDireita",
            cavaleiroEsquerda = "cavaleiroEsquerda";
    String magoDireita = "magoDireita", magoEsquerda = "magoEsquerda", magoCima = "magoCima", magoBaixo = "magoBaixo";
    String andarilhoDireita = "andarilhoDireita", andarilhoEsquerda = "andarilhoEsquerda",
            andarilhoCima = "andarilhoCima", andarilhoBaixo = "andarilhoBaixo";
    String elfoDireita = "elfoDireita", elfoEsquerda = "elfoEsquerda", elfoCima = "elfoCima", elfoBaixo = "elfoBaixo";
    String bauDireita = "bauDireita", bauEsquerda = "bauEsquerda", bauCima = "bauCima", bauBaixo = "bauBaixo";
    String bruxaDireita = "bruxaDireita", bruxaEsquerda = "bruxaEsquerda", bruxaCima = "bruxaCima",
            bruxaBaixo = "bruxaBaixo";
    String jenovaDireita = "jenovaDireita", jenovaEsquerda = "jenovaEsquerda", jenovaCima = "jenovaCima",
            jenovaBaixo = "jenovaBaixo";
    String verdeDireita = "verdeDireita", verdeEsquerda = "verdeEsquerda", verdeBaixo = "verdeBaixo",
            verdeCima = "verdeCima";
    final int HORIZONTAL = 0, VERTICAL = 1, HORIZONTAL_VERTICAL = 2;

    //// DADOS ITENS
    ArrayList<Itens> arrayItens = new ArrayList<>(2);
    final int ITEM_BOTA = 0, ITEM_TAMANHOEXPLOSAO = 1, ITEM_QTDEBOMBAS = 2, ITEM_VIDA = 3;
    int indexItems = 0;

    //// DADOS BOMBAS
    ArrayList<Bomba> arrayBombas = new ArrayList<>(10);
    ArrayList<pontoBomba> arrayExplosao = new ArrayList<>(110); // Fogo da explosão da bomba

    //// DADOS GERAIS
    final int TEMPO_DA_FASE = 150;
    boolean boolGameOver = false, boolLastBombaBlockPlayer = false, boolLastBombaBlockInimigo;
    float tempoCont;
    Timer tempo;
    //// DADOS DAS FASES
    final int MULTIPLAYER1 = 0, MULTIPLAYER2 = 1, MULTIPLAYER3 = 2, MULTIPLAYER4 = 3;
    boolean passarFase = false, single = false, multiplay = false, ultimaFase = false, escreveu = false;
    FaseMultiplayer mult;

    //////////////////////////////////////////////////////

    Timer refreshModels = new Timer(25, e -> {
        int i;
        try {
            //// Atualiza a imagem dos itens
            indexItems++;
            if (indexItems > 6)
                indexItems = 0;

            //// Atualiza a imagem da bomba
            if (arrayBombas.size() > 0) {
                for (i = 0; i < arrayBombas.size(); i++) {
                    arrayBombas.get(i).indexImage++;
                }
            }

            //// Movimenta os inimigos
            if (mult.arrayInimigos.size() > 0) {
                for (i = 0; i < mult.arrayInimigos.size(); i++) {
                    mult.arrayInimigos.get(i).move();
                }
            }

            // Checa as bombas recém colocadas e quando elas irão bloquear os players
            if (boolLastBombaBlockPlayer) {
                if (!arrayBombas.isEmpty()) {
                    for(i = 0; i < 4; i++){
                        if (!new Rectangle(arrayPlayerThread[i].X, arrayPlayerThread[i].Y + 15, 35, 40).intersects(arrayBombas.get(arrayBombas.size() - 1).getHitBox())) {
                        arrayBombas.get(arrayBombas.size() - 1).boolBloqueandoPlayer = true;
                        boolLastBombaBlockPlayer = false;
                        }
                    }
                    
                } else {
                    boolLastBombaBlockPlayer = false;
                }
            }

            // Checa as bombas recém colocadas e quando elas irão bloquear os inimigos (NPC)
            if (boolLastBombaBlockInimigo) {
                if (!arrayBombas.isEmpty()) {
                    for (i = 0; i < arrayBombas.size(); i++) {
                        for (int j = 0; j < mult.arrayInimigos.size(); j++) {
                            if (new Rectangle(mult.arrayInimigos.get(j).x, mult.arrayInimigos.get(j).y, 49, 49)
                                    .intersects(arrayBombas.get(i).getHitBox())) {
                                arrayBombas.get(i).boolBloqueandoInimigo = false;
                                boolLastBombaBlockInimigo = true;
                                break;
                            } else {
                                arrayBombas.get(i).boolBloqueandoInimigo = true;
                                boolLastBombaBlockInimigo = false;
                            }
                        }
                    }

                }
            }

        } catch (Exception eRef) {
            System.out.println("Erro no refreshModels: " + eRef);
        }
    });

    void checkPlayerEnemyColision() {
        for (int i = 0; i < mult.arrayInimigos.size(); i++) {
            for (int j = 0; j < 2 ; j++){
                if (arrayPlayerThread[i] != null && !arrayPlayerThread[i].boolDanoRecente && arrayPlayerThread[i].getHitBox().intersects(mult.arrayInimigos.get(i).getBounds())) {
                    arrayPlayerThread[i].danificado();
            }
        }
            if (mult.arrayInimigos.isEmpty())
                break;
        }
    }

    void damageDelayControl() {
        for (int i = 0 ; i < 2; i++){
            if (arrayPlayerThread[i] != null && arrayPlayerThread[i].boolDanoRecente) {
                if (arrayPlayerThread[i].danoRecente++ == 0) {
                    arrayPlayerThread[i].boolStunned = true;
                }
                if (arrayPlayerThread[i].danoRecente >= 20) { // numero de "ticks" de imobilização
                    arrayPlayerThread[i].boolStunned = false;
                }
                if (arrayPlayerThread[i].danoRecente >= 60) { // numero de "ticks" para que possa tomar outro dano, 40 ticks por segundo
                    arrayPlayerThread[i].boolDanoRecente = false;
                    arrayPlayerThread[i].danoRecente = 0;
                }
            }
        }
        
    }

    void checkPlayerItemColision() {
        for (int i = 0; i < arrayItens.size(); i++) {
            for (int j = 0 ; j < 2 ; j++){
                if (arrayPlayerThread[i] != null && arrayPlayerThread[i].getHitBox().intersects(arrayItens.get(i).getBounds())) {
                    if (arrayItens.get(i).item == ITEM_BOTA) {
                        arrayPlayerThread[i].qtdeItemBota++;
                        arrayPlayerThread[i].velocidade++; // Se houver uma intersecção do player com o item, incrementa velocidade
                    } else if (arrayItens.get(i).item == ITEM_TAMANHOEXPLOSAO) {
                        arrayPlayerThread[i].qtdeItemExplosao++;
                        arrayPlayerThread[i].bombaSize++;
                    } else if (arrayItens.get(i).item == ITEM_QTDEBOMBAS) {
                        arrayPlayerThread[i].qtdeItemBomba++;
                        arrayPlayerThread[i].maxBombas++;
                    } else {
                        if (arrayPlayerThread[i].vida < 3) {
                            arrayPlayerThread[i].vida++;
                        }
                    }
                    arrayItens.remove(i);
                    i = 0;
                    if(arrayItens.isEmpty())
                        break;
                    continue;
                }
            }
    }
}

    class FaseMultiplayer extends JPanel {
        ArrayList<Rectangle> arrayBlocosQuebraveis;
        ArrayList<Inimigo> arrayInimigos;
        Rectangle[] blocosFixos;
        Rectangle colisaoPorta = new Rectangle(450, 300, 55, 55);
        final int FUNDO = 0, BLOCO = 1, BLOCOQUEBRAVEL = 2, MARGEM_CE = 3, MARGEM_C = 4, MARGEM_CD = 5, MARGEM_E = 6,
                MARGEM_D = 7, MARGEM_BE = 8, MARGEM_B = 9, MARGEM_BD = 10;
        final int BLOCOSHORIZONTAIS = 8, BLOCOSVERTICAIS = 5;

        FaseMultiplayer(int numeroFase) {
            try {
                if (numeroFase == MULTIPLAYER1) {
                    funcAdcBlocosFixos(MULTIPLAYER1);
                    funcAdcBlocosQuebraveis(MULTIPLAYER1);
                    funcAdcItens(1, 2, 2, 1);
                    funcAdcInimigos(MULTIPLAYER1);
                } else {
                    //MULTIPLAYER2 MULTIPLAYER3 MULTIPLAYER4
                }
            } catch (Exception e) {
                System.out.println("Erro Construtor Fases: " + e);
            }
        }
        
        void funcAdcBlocosFixos(int mult) {
            try {
                blocosFixos = new Rectangle[40];
                int i, j, index = 0;
                if (mult == MULTIPLAYER1) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                } else if (mult == MULTIPLAYER2) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                } else if (mult == MULTIPLAYER3) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                } else if (mult == MULTIPLAYER4) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Erro Blocos Fixos: " + e);
            }
        }

        void funcAdcBlocosQuebraveis(int mult) { // Cria o array dos blocos quebráveis da Fase
        try {
            arrayBlocosQuebraveis = new ArrayList<>(60);
            int y;
            if (mult == MULTIPLAYER1) {
                y = 50; // LINHA 1
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
            } else if (mult == MULTIPLAYER2) {
                y = 50; // LINHA 1
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 100; // LINHA 2
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                y = 150; // LINHA 3
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 200; // LINHA 4
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 250; // LINHA 5
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 300; // LINHA 6
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                y = 350; // LINHA 7
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                y = 400; // LINHA 8
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 450; // LINHA 9
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                y = 500; // LINHA 10
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                y = 550; // LINHA 11
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(300, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
            } else if (mult == MULTIPLAYER3) {
                y = 50; // LINHA 1
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 100; // LINHA 2
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                y = 150; // LINHA 3
                arrayBlocosQuebraveis.add(new Rectangle(100, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 200; // LINHA 4
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                y = 250; // LINHA 5
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 300; // LINHA 6
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 350; // LINHA 7
                arrayBlocosQuebraveis.add(new Rectangle(100, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 400; // LINHA 8
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 450; // LINHA 9
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(300, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                y = 500; // LINHA 10
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 550; // LINHA 11
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
            } else if (mult == MULTIPLAYER4) {
                y = 50; // LINHA 1
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 100; // LINHA 2
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 150; // LINHA 3
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 200; // LINHA 4
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                y = 250; // LINHA 5
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                y = 300; // LINHA 6
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 350; // LINHA 7
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(100, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                y = 400; // LINHA 8
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 450; // LINHA 9
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(300, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                y = 500; // LINHA 10
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 550; // LINHA 11
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
            }
        } catch (Exception e) {
            System.out.println("Erro Blocos Quebraveis: " + e);
        }
    }

        void funcAdcItens(int itemBota, int itemTamanhoExplosao, int itemQtdeBombas, int itemVida) {
        try {
            arrayItens = new ArrayList<>(itemBota + itemTamanhoExplosao + itemQtdeBombas + itemVida);
            int i;
            int[] posicaoDosItens = new int[itemBota + itemTamanhoExplosao + itemQtdeBombas + itemVida]; // guarda a
                                                                                                            // posição
                                                                                                            // dos
                                                                                                            // itens,
                                                                                                            // tamanho
                                                                                                            // do vetor
                                                                                                            // é qtde
                                                                                                            // de itens
                                                                                                            // max
            Random numeroAleatorio = new Random();
            int indexPosicoes = 0, numeroAleatorioItemAux;

            for (i = 0; i < itemBota; i++) {
                if (indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                    break;
                do {
                    numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um
                                                                                                    // numero
                                                                                                    // aleatorio
                } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                arrayItens.add(new Itens(ITEM_BOTA, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(),
                        (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                indexPosicoes++;
            }

            for (i = 0; i < itemTamanhoExplosao; i++) {
                if (indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                    break;
                do {
                    numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um
                                                                                                    // numero
                                                                                                    // aleatorio
                } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                arrayItens.add(new Itens(ITEM_TAMANHOEXPLOSAO,
                        (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(),
                        (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                indexPosicoes++;
            }

            for (i = 0; i < itemQtdeBombas; i++) {
                if (indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                    break;
                do {
                    numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um
                                                                                                    // numero
                                                                                                    // aleatorio
                } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                arrayItens.add(
                        new Itens(ITEM_QTDEBOMBAS, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(),
                                (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                indexPosicoes++;
            }

            for (i = 0; i < itemVida; i++) {
                if (indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                    break;
                do {
                    numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um
                                                                                                    // numero
                                                                                                    // aleatorio
                } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                arrayItens.add(new Itens(ITEM_VIDA, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(),
                        (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                indexPosicoes++;
            }
        } catch (Exception e) {
            System.out.println("Erro Adc Itens: " + e);
        }
    }

        void funcAdcInimigos(int mult) {
        try {
            arrayInimigos = new ArrayList<>(10);
            if (mult == MULTIPLAYER1) {

            } else if (mult == MULTIPLAYER2) {
                arrayInimigos.add(new Inimigo(400, 50, 2, HORIZONTAL, morcegoDireita, morcegoEsquerda));
                arrayInimigos.add(new Inimigo(850, 300, 2, VERTICAL, morcegoCima, morcegoBaixo));
                arrayInimigos.add(new Inimigo(250, 250, 2, HORIZONTAL, magoDireita, magoEsquerda));
                arrayInimigos.add(new Inimigo(250, 50, 1, HORIZONTAL, cavaleiroDireita, cavaleiroEsquerda));
                arrayInimigos.add(new Inimigo(750, 250, 2, VERTICAL, magoCima, magoBaixo));
                arrayInimigos.add(new Inimigo(450, 250, 1, VERTICAL, cavaleiroCima, cavaleiroBaixo));
            } else if (mult == MULTIPLAYER3) {
                arrayInimigos.add(new Inimigo(550, 550, 2, HORIZONTAL, bauDireita, bauEsquerda));
                arrayInimigos.add(new Inimigo(250, 250, 2, VERTICAL, bauCima, bauBaixo));
                arrayInimigos.add(new Inimigo(200, 50, 2, HORIZONTAL, jenovaDireita, jenovaEsquerda));
                arrayInimigos.add(new Inimigo(550, 450, 2, VERTICAL, jenovaCima, jenovaBaixo));
                arrayInimigos.add(new Inimigo(150, 150, 2, HORIZONTAL, bruxaDireita, bruxaEsquerda));
                arrayInimigos.add(new Inimigo(650, 150, 2, VERTICAL, bruxaCima, bruxaBaixo));
            } else if (mult == MULTIPLAYER4) {
                arrayInimigos.add(new Inimigo(300, 550, 2, HORIZONTAL, verdeDireita, verdeEsquerda));
                arrayInimigos.add(new Inimigo(650, 250, 2, VERTICAL, verdeCima, verdeBaixo));
                arrayInimigos.add(new Inimigo(200, 450, 2, HORIZONTAL, elfoDireita, elfoEsquerda));
                arrayInimigos.add(new Inimigo(550, 450, 2, VERTICAL, elfoCima, elfoBaixo));
                arrayInimigos.add(new Inimigo(150, 150, 2, HORIZONTAL, andarilhoDireita, andarilhoEsquerda));
                arrayInimigos.add(new Inimigo(850, 150, 2, VERTICAL, andarilhoCima, andarilhoBaixo));
            }
        } catch (Exception e) {
            System.out.println("Erro Adc Inimigos: " + e);
        }
    }

      boolean funcChecaPosItens(int valor, int[] arrayDosItens) { // Serve para não repetir itens na mesma posição
            for (int arrayDosIten : arrayDosItens) {
                if (valor == arrayDosIten) {
                    return true;
                }
            }
            return false;
        }
    }

    boolean intersBombas(Rectangle checkR) { // Movimentação player vs bomba
        for (Bomba bombaBlock : arrayBombas) {
            if (bombaBlock.boolBloqueandoPlayer && checkR.intersects(bombaBlock.getHitBox()))
                return false;
        }
        return true;
    }

    boolean intersBombasInimigos(Rectangle checkR) { // Movimentação inimigos vs bomba
        for (Bomba bombaBlock : arrayBombas) {
            if (bombaBlock.boolBloqueandoInimigo && checkR.intersects(bombaBlock.getHitBox()))
                return false;
        }
        return true;
    }

    boolean intersBlocosFixos(Rectangle checkR, Rectangle[] blocosFixos) { // Retorna TRUE se não há colisão
        for (int i = 0; i < 40; i++) {
            if (checkR.intersects(blocosFixos[i])) {
                return false;
            }
        }
        return true;
    }

    boolean intersItem(Rectangle checkR) { // Retorna TRUE se não há colisão
        for (Itens c : arrayItens) {
            if (checkR.intersects(c.getBounds())) {
                return false;
            }
        }
        return true;
    }

    boolean intersBlocosQuebraveis(Rectangle checkR, ArrayList<Rectangle> arrayBlocosQuebraveis) { // Retorna TRUE se não há colisão
        for (Rectangle arrayBlocosQuebravei : arrayBlocosQuebraveis) {
            if (checkR.intersects(arrayBlocosQuebravei)) {
                return false;
            }
        }
        return true;
    }

    boolean inimigoIntersX(Rectangle checkR, ArrayList<Rectangle> arrayBlocosQuebraveis) { // Retorna TRUE se não há colisão
        for (Rectangle arrayBlocosQuebravei : arrayBlocosQuebraveis) {
            if (arrayBlocosQuebravei.getY() == checkR.getY()) {
                if (arrayBlocosQuebravei.intersects(checkR) && arrayBlocosQuebravei.intersects(checkR)) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean inimigoIntersY(Rectangle checkR, ArrayList<Rectangle> arrayBlocosQuebraveis) { // Retorna TRUE se não há colisão
        for (Rectangle arrayBlocosQuebravei : arrayBlocosQuebraveis) {
            if (arrayBlocosQuebravei.getX() == checkR.getX()) {
                if (arrayBlocosQuebravei.intersects(checkR) && arrayBlocosQuebravei.intersects(checkR)) {
                    return false;
                }
            }
        }
        return true;
    }

    void funcExplodeBomba(Bomba bomba) {
        int expX = bomba.getX(), expY = bomba.getY();
        arrayExplosao.add(new pontoBomba(expX, expY, 0));
        for (int i = 0, fogoSobe = expY - 50; fogoSobe >= 50 && i < bomba.valorBombaSize
                && intersBlocosFixos(new Rectangle(expX, fogoSobe, 50, 50), mult.blocosFixos); fogoSobe -= 50, i++) {
            if (intersBlocosQuebraveis((new Rectangle(expX, fogoSobe, 50, 50)), mult.arrayBlocosQuebraveis)) {
                arrayExplosao.add(new pontoBomba(expX, fogoSobe, 0));
            } else {
                arrayExplosao.add(new pontoBomba(expX, fogoSobe, 1));
                break;
            }
        }
        for (int i = 0, fogoDesce = expY + 50; fogoDesce <= 550 && i < bomba.valorBombaSize
                && intersBlocosFixos(new Rectangle(expX, fogoDesce, 50, 50), mult.blocosFixos); fogoDesce += 50, i++) {
            if (intersBlocosQuebraveis((new Rectangle(expX, fogoDesce, 50, 50)), mult.arrayBlocosQuebraveis)) {
                arrayExplosao.add(new pontoBomba(expX, fogoDesce, 0));
            } else {
                arrayExplosao.add(new pontoBomba(expX, fogoDesce, 1));
                break;
            }
        }
        for (int i = 0, fogoEsquerda = expX - 50; fogoEsquerda >= 50 && i < bomba.valorBombaSize && intersBlocosFixos(
                new Rectangle(fogoEsquerda, expY, 50, 50), mult.blocosFixos); fogoEsquerda -= 50, i++) {
            if (intersBlocosQuebraveis((new Rectangle(fogoEsquerda, expY, 50, 50)), mult.arrayBlocosQuebraveis)) {
                arrayExplosao.add(new pontoBomba(fogoEsquerda, expY, 0));
            } else {
                arrayExplosao.add(new pontoBomba(fogoEsquerda, expY, 1));
                break;
            }
        }
        for (int i = 0, fogoDireita = expX + 50; fogoDireita <= 850 && i < bomba.valorBombaSize && intersBlocosFixos(
                new Rectangle(fogoDireita, expY, 50, 50), mult.blocosFixos); fogoDireita += 50, i++) {
            if (intersBlocosQuebraveis((new Rectangle(fogoDireita, expY, 50, 50)), mult.arrayBlocosQuebraveis)) {
                arrayExplosao.add(new pontoBomba(fogoDireita, expY, 0));
            } else {
                arrayExplosao.add(new pontoBomba(fogoDireita, expY, 1));
                break;
            }
        }
    }

    static class pontoBomba extends Point {
        int holdDraw; // int que será decrementado durante a chamada Draw pra garantir com que a
                      // imagem seja desenhada holdDraw vezes
        int tipoDeAnimacao; // será 0 para a explosão normal e 1 para explosões que sobrepuserem os blocos
                            // quebráveis
        Rectangle hitBox;

        pontoBomba(int x, int y, int tipo) {
            this.x = x;
            this.y = y;
            hitBox = new Rectangle(x, y, 50, 50);
            holdDraw = 30;
            tipoDeAnimacao = tipo;
        }
    }

    class Bomba {
        int x, y, valorBombaSize, indexImage = 0, dono;
        Image[] arrayImagensBomba;
        Rectangle hitBox;
        boolean boolBloqueandoPlayer = false, boolBloqueandoInimigo = false;

        Bomba(int x, int y, int bombaSize) {
            carregaImagens();
            valorBombaSize = bombaSize;
            // Setar o bombaY no centro dos espaços
            if (y < 310) { // Divisão do Y na metade para possivelmente acelerar a chegada do if
                           // correspondente, como uma busca binária no início
                if (y >= 25 && y <= 60)
                    this.y = 50;
                else if (y <= 110)
                    this.y = 100;
                else if (y <= 160)
                    this.y = 150;
                else if (y <= 210)
                    this.y = 200;
                else if (y <= 260)
                    this.y = 250;
                else
                    this.y = 300;
            } else {
                if (y <= 360)
                    this.y = 350;
                else if (y <= 410)
                    this.y = 400;
                else if (y <= 460)
                    this.y = 450;
                else if (y <= 510)
                    this.y = 500;
                else if (y <= 560)
                    this.y = 550;
            }
            // Setar o bombaX no centro dos espaços
            if (x <= 535) { // Divisão do X na metade para possivelmente acelerar a chegada do if
                            // correspondente, como uma busca binária no início
                if (x <= 85)
                    this.x = 50;
                else if (x <= 135)
                    this.x = 100;
                else if (x <= 185)
                    this.x = 150;
                else if (x <= 235)
                    this.x = 200;
                else if (x <= 285)
                    this.x = 250;
                else if (x <= 335)
                    this.x = 300;
                else if (x <= 385)
                    this.x = 350;
                else if (x <= 435)
                    this.x = 400;
                else if (x <= 485)
                    this.x = 450;
                else
                    this.x = 500;
            } else {
                if (x <= 585)
                    this.x = 550;
                else if (x <= 635)
                    this.x = 600;
                else if (x <= 685)
                    this.x = 650;
                else if (x <= 735)
                    this.x = 700;
                else if (x <= 785)
                    this.x = 750;
                else if (x <= 835)
                    this.x = 800;
                else
                    this.x = 850;
            }
            hitBox = new Rectangle(this.x + 12, this.y + 12, 26, 26);
            boolLastBombaBlockPlayer = boolLastBombaBlockInimigo = true;
        }

        public Rectangle getHitBox() {
            return new Rectangle(this.x, this.y, 50, 50);
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        void carregaImagens() {
            try {
                arrayImagensBomba = new Image[21];
                for (int i = 0; i < 20; i++) {
                    arrayImagensBomba[i] = (new ImageIcon("Resources/Frames/bomb" + i + ".png").getImage());
                }
            } catch (Exception b) {
                System.out.println("Erro bomba: " + b);
            }
        }
    }

    class Inimigo {
        final int HORIZONTAL = 0;
        int x, y, sentido;
        Image inimigo;
        ImageIcon direita;
        ImageIcon esquerda;
        ImageIcon cima;
        ImageIcon baixo;
        int velocidade;
        boolean inverteMovimento = false;

        Inimigo(int xInicial, int yInicial, int valorVelocidade, int valorSentido, String gifInimigo1,
                String gifInimigo2) {
            x = xInicial;
            y = yInicial;
            if (valorSentido == HORIZONTAL) {
                direita = new ImageIcon("Resources//Enemies//" + gifInimigo1 + ".gif");
                esquerda = new ImageIcon("Resources//Enemies//" + gifInimigo2 + ".gif");
            } else if (valorSentido == VERTICAL) {
                cima = new ImageIcon("Resources//Enemies//" + gifInimigo1 + ".gif");
                baixo = new ImageIcon("Resources//Enemies//" + gifInimigo2 + ".gif");
            } else if (valorSentido == HORIZONTAL_VERTICAL) {
                direita = new ImageIcon("Resources//Enemies//" + gifInimigo1 + "Direita" + ".gif");
                esquerda = new ImageIcon("Resources//Enemies//" + gifInimigo2 + "Esquerda" + ".gif");
                cima = new ImageIcon("Resources//Enemies//" + gifInimigo1 + "Cima" + ".gif");
                baixo = new ImageIcon("Resources//Enemies//" + gifInimigo2 + "Baixo" + ".gif");
            }
            this.velocidade = valorVelocidade;
            this.sentido = valorSentido;
        }

        void move() {
            int XdaBomba;
            if (sentido == HORIZONTAL) {
                // DIREITA, INVERTEMOVIMENTO = FALSE
                if (x <= 850 && !inverteMovimento
                        && inimigoIntersX(new Rectangle(x + velocidade, y, 40, 40), mult.arrayBlocosQuebraveis)
                        && intersBombasInimigos(new Rectangle(x + velocidade, y, 40, 40))) {
                    inimigo = direita.getImage();
                    x += velocidade;
                } else {
                    inverteMovimento = true;
                }
                // ESQUERDA, INVERTEMOVIMENTO = TRUE
                if (x >= 50 && inverteMovimento
                        && inimigoIntersX(new Rectangle(x - velocidade, y, 40, 40), mult.arrayBlocosQuebraveis)
                        && intersBombasInimigos(new Rectangle(x - velocidade, y, 40, 40))) {
                    inimigo = esquerda.getImage();
                    x -= velocidade;
                } else {
                    inverteMovimento = false;
                }
            } else { // SENTIDO VERTICAL
                // PARA BAIXO, INVERTEMOVIMENTO = FALSO
                if (y <= 550 && !inverteMovimento
                        && inimigoIntersY(new Rectangle(x, y + velocidade, 40, 40), mult.arrayBlocosQuebraveis)
                        && intersBombasInimigos(new Rectangle(x, y + velocidade, 40, 40))) {
                    inimigo = baixo.getImage();
                    y += velocidade;
                } else {
                    inverteMovimento = true;
                }
                // PARA CIMA, INVERTEMOVIMENTO = TRUE
                if (y >= 50 && inverteMovimento
                        && inimigoIntersY(new Rectangle(x, y - velocidade, 40, 40), mult.arrayBlocosQuebraveis)
                        && intersBombasInimigos(new Rectangle(x, y - velocidade, 40, 40))) {
                    inimigo = cima.getImage();
                    y -= velocidade;
                } else {
                    inverteMovimento = false;
                }
            }
        }

        public Image getImage() {
            return inimigo;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, 31, 47);
        }
    }

    class Itens {
        ImageIcon[] imagensItemAnimacao = new ImageIcon[7];
        Image imagem;
        boolean boolItemRecemQueimado = false;
        int item, recemQueimado = 0;
        int itemQueimado = 2; // quando a bomba queimar o item que estiver descoberto ele some (1- aparece
                              // 0-queima)
        int posX, posY;

        Itens(int tipoItem, int posX, int posY) {
            int i;
            if (tipoItem == 0) {
                for (i = 0; i < 7; i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/BotaIMG" + i + ".png");
            } else if (tipoItem == 1) {
                for (i = 0; i < 7; i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/TamanhoExplosaoIMG" + i + ".png");
            } else if (tipoItem == 2) {
                for (i = 0; i < 7; i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/QtdeBombasIMG" + i + ".png");
            } else {
                for (i = 0; i < 7; i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/ItemVidaIMG" + i + ".png");
            }
            item = tipoItem;
            this.posX = posX;
            this.posY = posY;
        }

        public Rectangle getBounds() {
            return new Rectangle(this.posX, this.posY, 50, 50);
        }

    }

    class PlayerThread extends Thread {
        ////
        int vida = 3, danoRecente = 0, velocidade = 4, qtdeItemBota, qtdeItemBomba, qtdeItemExplosao;
        boolean boolDanoRecente = false, boolStunned = false, moveRight = false, moveLeft = false, moveDown = false, moveUp = false;
        int estado = PARADO, X = 60, Y = 40, maxBombas = 2, bombaSize = 1;
        int id = 0;
        //// alteração socket
        Socket playerSocket;
        DataOutputStream streamEnviaAoCliente;
        DataInputStream streamRecebeDoCliente;
        //BufferedReader reader;
        boolean boolTrocandoDados = false;
        String leitura;
        String[] leituraPartes;
        int bombasAtivas=0;

        PlayerThread(Socket socketRecebido) {
            try {
                this.playerSocket = socketRecebido;
                this.streamRecebeDoCliente = new DataInputStream(socketRecebido.getInputStream());
                this.streamEnviaAoCliente = new DataOutputStream(socketRecebido.getOutputStream());
                //this.reader = new BufferedReader(new InputStreamReader(streamRecebeDoCliente));

            } catch (Exception erroPlayer) {
                System.out.println("Erro (Player): " + erroPlayer);
            }
        }

        public void run() {
            try {
                System.out.println("\nPlayer run");

                while(!boolIniciaJogo){
                }//While para segurar a thread

                System.out.println("\nTrocando Dados = true");

                streamEnviaAoCliente.writeUTF ("JP");
                streamEnviaAoCliente.flush();
                while(true){
                    if(!boolTrocandoDados)
                        boolTrocandoDados = true;

                    ///// RECEBIMENDO DOS DADOS DO CLIENTE
                    //recebe as bombas
                    leitura = streamRecebeDoCliente.readUTF();
                    leituraPartes = leitura.split(" ");
                    if(leituraPartes[0].startsWith("BOM")){
                        if(bombasAtivas < maxBombas){
                            bombasAtivas++;
                            arrayBombas.add(new Bomba(Integer.parseInt(leituraPartes[1]), Integer.parseInt(leituraPartes[2]), Integer.parseInt(leituraPartes[3])));
                            //recebe:
                        }
                    } else if (leituraPartes[0].startsWith("POS")){
                        X = Integer.parseInt(leituraPartes[1]);
                        Y = Integer.parseInt(leituraPartes[2]);
                        estado = Integer.parseInt(leituraPartes[3]);
                        //recebe:("POS "+arrayPlayers[0].getX()+" "+arrayPlayers[0].getY()+" "+arrayPlayers[0].estado)
                    }

                    ///// ENVIO DOS DADOS AO CLIENTE
                    //envia ao cliente posicoes
                    streamEnviaAoCliente.writeUTF("POS ");
                    if(id == 0){
                        streamEnviaAoCliente.writeUTF("P"+1+" "+arrayPlayerThread[1].X+" "+arrayPlayerThread[1].Y);
                    } else {
                        streamEnviaAoCliente.writeUTF("P"+0+" "+arrayPlayerThread[0].X+" "+arrayPlayerThread[0].Y);
                    }

                    //envia ao cliente o array das bombas
                    if(!arrayBombas.isEmpty()){
                        for(int i=0 ; i<arrayBombas.size() ; i++){
                            streamEnviaAoCliente.writeUTF("BMB "+arrayBombas.get(i).dono+" "+arrayBombas.get(i).x+" "+arrayBombas.get(i).y+" "+arrayBombas.get(i).indexImage+" "+arrayBombas);
                         }
                    }
                    
                }
            }
            catch(NoSuchElementException e){
            }
            catch(Exception ex){
            }

            try {
                streamEnviaAoCliente.close();
                streamRecebeDoCliente.close();
                playerSocket.close();
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        Rectangle getHitBox(){ //hitbox do player
            return new Rectangle(this.X,this.Y+15,30,35);
        }

        void danificado(){
            this.vida--;
            this.boolDanoRecente = true;
            this.danoRecente = 0;

            if(this.velocidade>4) {
                this.velocidade--;
                this.qtdeItemBota--;
            }
            if(this.bombaSize>1) {
                this.bombaSize--;
                this.qtdeItemExplosao--;
            }
            if(this.maxBombas > 2) {
                this.maxBombas--;
                this.qtdeItemBomba--;
            }
        }

        public void envia (String s){
            try {
                streamEnviaAoCliente.writeUTF(s);
            } catch (IOException e) {
                System.out.println("Erro aqui 2");
            }
        }

    }

    ServidorTESTE() {
        try{
            serverSocket = new ServerSocket(PORTO);
            // CONEXÃO
            System.out.println("\nAguardando primeira conexao.");
            arrayPlayerSockets[0] = serverSocket.accept();
            System.out.println("\nPlayer 1 conectado. Enviando o clientSocket ao PlayerThread.");
            arrayPlayerThread[0] = new PlayerThread(arrayPlayerSockets[0]);
            System.out.println("\nIniciando a thread do Player 1.");
            arrayPlayerThread[0].start();
            arrayPlayerThread[0].id = 0;
        }
        catch(Exception e){
            System.out.println("\nErro na conexao 1. - "+e);
            System.exit(1);
        }


        try{
            serverSocket = new ServerSocket(PORTO);
            // CONEXÃO
            System.out.println("\nAguardando segunda conexao.");
            arrayPlayerSockets[1] = serverSocket.accept();
            System.out.println("\nPlayer 2 conectado. Enviando o clientSocket ao PlayerThread.");
            arrayPlayerThread[1] = new PlayerThread(arrayPlayerSockets[10]);
            System.out.println("\nIniciando a thread do Player 2.");
            arrayPlayerThread[1].start();
            arrayPlayerThread[1].id = 1;
        }
        catch(Exception e){
            System.out.println("\nErro na conexao 2. - "+e);
            System.exit(1);
        }

        boolIniciaJogo = true;
}

    static public void main(String[] args) throws InterruptedException {
        new ServidorTESTE();
    }
}