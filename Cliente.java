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
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

class Cliente extends JFrame {
    /// Rede
    Rede rede = new Rede(this, "127.0.0.1", 12345);
    //// DADOS PLAYER
    final int PERS1 = 1, PERS2 = 2, PERS3 = 3, PERS4 = 4;
    Player arrayPlayers[] = {new Player(PERS1), new Player(PERS2), new Player(PERS3), new Player(PERS4)};
    //Player currentPlayer = new Player(PERS1);
    final int PARADO = 0, ANDANDO_DIREITA = 1,ANDANDO_ESQUERDA = 2, ANDANDO_FRENTE = 3, ANDANDO_COSTAS = 4, DANIFICADO = 5, LENGTH_IMAGENS_PLAYER = 6;
    String nome_do_Player, score_do_Player = null;

    //// DADOS INIMIGO
    String morcegoDireita = "morcegoDireita", morcegoEsquerda = "morcegoEsquerda", morcegoCima = "morcegoCima", morcegoBaixo = "morcegoBaixo";
    String cavaleiroBaixo = "cavaleiroBaixo", cavaleiroCima = "cavaleiroCima", cavaleiroDireita = "cavaleiroDireita", cavaleiroEsquerda = "cavaleiroEsquerda";
    String magoDireita = "magoDireita", magoEsquerda = "magoEsquerda", magoCima = "magoCima", magoBaixo = "magoBaixo";
    String andarilhoDireita = "andarilhoDireita", andarilhoEsquerda = "andarilhoEsquerda", andarilhoCima = "andarilhoCima", andarilhoBaixo = "andarilhoBaixo";
    String elfoDireita = "elfoDireita", elfoEsquerda = "elfoEsquerda", elfoCima = "elfoCima", elfoBaixo = "elfoBaixo";
    String bauDireita = "bauDireita", bauEsquerda = "bauEsquerda", bauCima = "bauCima", bauBaixo = "bauBaixo";
    String bruxaDireita = "bruxaDireita", bruxaEsquerda = "bruxaEsquerda", bruxaCima = "bruxaCima", bruxaBaixo = "bruxaBaixo";
    String jenovaDireita = "jenovaDireita", jenovaEsquerda = "jenovaEsquerda", jenovaCima = "jenovaCima", jenovaBaixo = "jenovaBaixo";
    String verdeDireita = "verdeDireita", verdeEsquerda = "verdeEsquerda", verdeBaixo = "verdeBaixo", verdeCima = "verdeCima";
    final int HORIZONTAL = 0, VERTICAL = 1, HORIZONTAL_VERTICAL = 2;

    //// DADOS ITENS
    ArrayList<Itens> arrayItens;
    final int ITEM_BOTA = 0, ITEM_TAMANHOEXPLOSAO = 1, ITEM_QTDEBOMBAS = 2, ITEM_VIDA =3;
    int indexItems = 0;

    //// DADOS BOMBAS
    ArrayList<Bomba> arrayBombas;
    ArrayList<pontoBomba> arrayExplosao; //Fogo da explosão da bomba


    //// DADOS GERAIS
    int gameControler;
    int endGame = 2;
    int somaScore, scoreVida = 300, scoreBloco;
    final int TEMPO_DA_FASE = 150, NAV_MENU = 10, NAV_MULTIPLAYER = 0, NAV_FASE1 = 1, NAV_FASE2 = 2, NAV_FASE3 = 3;
    boolean boolGameOver = false, boolLastBombaBlockPlayer = false, boolLastBombaBlockInimigo;
    Image gameOver = new ImageIcon("Resources/Layout/gameover.gif").getImage();
    ImageIcon coracaoCheio = new ImageIcon("Resources/Layout/coracaoCheio.png");
    ImageIcon coracaoVazio = new ImageIcon("Resources/Layout/coracaoVazio.png");
    Image explosao = (new ImageIcon("Resources/Frames/expl.gif")).getImage();
    Image explosao2 = (new ImageIcon("Resources/Frames/expl2.gif")).getImage(); // explosao2 é a explosão em cima dos blocos quebráveis
    JLabel primeiroCoracao = new JLabel();
    JLabel segundoCoracao = new JLabel();
    JLabel terceiroCoracao = new JLabel();
    Font fonte = new Font("Arial", Font.BOLD, 30);

    //// DADOS GUI
    Menu menu;
    layoutDeCima barraSuperior;
    JLabel labelQuantidadeItemBota, labelQuantidadeItemBomba, labelQuantidadeItemExplosao;
    JLabel labelTempoValue ,labelScore;

    //// DADOS DAS FASES
    final int MULTIPLAYER1 = 0, MULTIPLAYER2 = 1, MULTIPLAYER3 = 2, MULTIPLAYER4 = 3;
    boolean passarFase = false, ultimaFase = false, escreveu = false;
    Fase mult;
    Fase fase;
    boolean single, multiplay;

    //////////////////////////////////////////////////////

    Timer refreshModels = new Timer(25, e -> {
        int i;
        try {
            
            //// Controlar o dano recente do player
            while (rede.continua()) {
                rede.recebePosicoesPlayers(arrayPlayers);
                String tipo = rede.recebeMensagem();
                if (tipo == "Vida") {
                    if (arrayPlayers[0].boolDanoRecente) {
                        if (arrayPlayers[0].danoRecente++ == 0) {
                            checkVida();
                            scoreVida -= 100;
                            arrayPlayers[0].personagem = arrayPlayers[0].imagensPlayer[DANIFICADO];
                            arrayPlayers[0].boolStunned = true;
                        }
                        if (arrayPlayers[0].danoRecente >= 20) { // numero de "ticks" de imobilização
                            arrayPlayers[0].boolStunned = false;
                        }
                        if (arrayPlayers[0].danoRecente >= 60) { // numero de "ticks" para que possa tomar outro dano, 40 ticks por segundo
                            arrayPlayers[0].boolDanoRecente = false;
                            arrayPlayers[0].danoRecente = 0;
                        }
                    }
                 }

                /////////// Movimentação do player
                if (tipo == "pos") {
                    if (!arrayPlayers[0].boolStunned && arrayPlayers[0].getVida() > 0) { // Se não tomou dano recente (stun) e está vivo
                        if (arrayPlayers[0].estado == PARADO) {
                            arrayPlayers[0].personagem = arrayPlayers[0].imagensPlayer[PARADO];
                        } else if (arrayPlayers[0].estado == ANDANDO_DIREITA) {
                            arrayPlayers[0].personagem = arrayPlayers[0].imagensPlayer[ANDANDO_DIREITA];
                        } else if (arrayPlayers[0].estado == ANDANDO_ESQUERDA) {
                            arrayPlayers[0].personagem = arrayPlayers[0].imagensPlayer[ANDANDO_ESQUERDA];
                        } else if (arrayPlayers[0].estado == ANDANDO_FRENTE) {
                            arrayPlayers[0].personagem = arrayPlayers[0].imagensPlayer[ANDANDO_FRENTE];
                        } else if (arrayPlayers[0].estado == ANDANDO_COSTAS) {
                            arrayPlayers[0].personagem = arrayPlayers[0].imagensPlayer[ANDANDO_COSTAS];
                        } else {
                            arrayPlayers[0].personagem = arrayPlayers[0].imagensPlayer[PARADO];
                        }
                        if (arrayPlayers[0].moveRight) { //DIREITA
                            if (intersBombas(new Rectangle(arrayPlayers[0].X + arrayPlayers[0].velocidade, arrayPlayers[0].Y + 15, 30, 35)) && intersBlocosFixos(new Rectangle(arrayPlayers[0].X + arrayPlayers[0].velocidade, arrayPlayers[0].Y + 15, 30, 35), fase.blocosFixos) && intersBlocosQuebraveis(new Rectangle(arrayPlayers[0].X + arrayPlayers[0].velocidade, arrayPlayers[0].Y + 15, 30, 35), fase.arrayBlocosQuebraveis) && arrayPlayers[0].X <= 856) {
                                arrayPlayers[0].X += arrayPlayers[0].velocidade;
                            }
                            if (arrayPlayers[0].estado != ANDANDO_DIREITA && !arrayPlayers[0].moveDown && !arrayPlayers[0].moveUp) {
                                arrayPlayers[0].estado = ANDANDO_DIREITA;
                            }
                        } else if (arrayPlayers[0].moveLeft) { //ESQUERDA
                            if (intersBombas(new Rectangle(arrayPlayers[0].X - arrayPlayers[0].velocidade, arrayPlayers[0].Y + 15, 30, 35)) && intersBlocosFixos(new Rectangle(arrayPlayers[0].X - arrayPlayers[0].velocidade, arrayPlayers[0].Y + 15, 30, 35), fase.blocosFixos) && intersBlocosQuebraveis(new Rectangle(arrayPlayers[0].X - arrayPlayers[0].velocidade, arrayPlayers[0].Y + 15, 30, 35), fase.arrayBlocosQuebraveis) && arrayPlayers[0].X >= 54) {
                                arrayPlayers[0].X -= arrayPlayers[0].velocidade;
                            }
                            if (arrayPlayers[0].estado != ANDANDO_ESQUERDA && !arrayPlayers[0].moveDown && !arrayPlayers[0].moveUp) {
                                arrayPlayers[0].estado = ANDANDO_ESQUERDA;
                            }
                        }
                        if (arrayPlayers[0].moveDown) { //BAIXO
                            if (intersBombas(new Rectangle(arrayPlayers[0].X, arrayPlayers[0].Y + arrayPlayers[0].velocidade + 15, 30, 35)) && intersBlocosFixos(new Rectangle(arrayPlayers[0].X, arrayPlayers[0].Y + arrayPlayers[0].velocidade + 15, 30, 35), fase.blocosFixos) && intersBlocosQuebraveis(new Rectangle(arrayPlayers[0].X, arrayPlayers[0].Y + arrayPlayers[0].velocidade + 15, 30, 35), fase.arrayBlocosQuebraveis) && arrayPlayers[0].Y <= 550) {
                                arrayPlayers[0].Y += arrayPlayers[0].velocidade;
                            }
                            if (arrayPlayers[0].estado != ANDANDO_FRENTE) {
                                arrayPlayers[0].estado = ANDANDO_FRENTE;
                            }
                        } else if (arrayPlayers[0].moveUp) { //CIMA
                            if (intersBombas(new Rectangle(arrayPlayers[0].X, arrayPlayers[0].Y - arrayPlayers[0].velocidade + 15, 30, 35)) && intersBlocosFixos(new Rectangle(arrayPlayers[0].X, arrayPlayers[0].Y - arrayPlayers[0].velocidade + 15, 30, 35), fase.blocosFixos) && intersBlocosQuebraveis(new Rectangle(arrayPlayers[0].X, arrayPlayers[0].Y - arrayPlayers[0].velocidade + 15, 30, 35), fase.arrayBlocosQuebraveis) && arrayPlayers[0].Y >= 30) {
                                arrayPlayers[0].Y -= arrayPlayers[0].velocidade;
                            }
                            if (arrayPlayers[0].estado != ANDANDO_COSTAS) {
                                arrayPlayers[0].estado = ANDANDO_COSTAS;
                            }
                        }
                        if (!arrayPlayers[0].moveDown && !arrayPlayers[0].moveUp && !arrayPlayers[0].moveLeft && !arrayPlayers[0].moveRight) { //PARADO
                            if (arrayPlayers[0].estado != PARADO) {
                                arrayPlayers[0].estado = PARADO;
                            }
                        }
                    }
                }
                repaint();
            }
        }catch (Exception eRef){
            System.out.println("Erro no refreshModels: "+eRef);
        }
    });

    class Fase extends JPanel{
        ArrayList<Rectangle> arrayBlocosQuebraveis;
        ArrayList<Inimigo> arrayInimigos;
        Image[] imagensAmbiente = new Image[11];
        Image doorClosed;
        Rectangle[] blocosFixos;
        Rectangle colisaoPorta = new Rectangle(450,300,55,55);
        final int FUNDO = 0, BLOCO = 1, BLOCOQUEBRAVEL = 2, MARGEM_CE = 3, MARGEM_C = 4, MARGEM_CD = 5,
                MARGEM_E = 6, MARGEM_D = 7, MARGEM_BE = 8, MARGEM_B = 9, MARGEM_BD = 10;
        final int BLOCOSHORIZONTAIS = 8, BLOCOSVERTICAIS = 5;
        Fase(int numeroFase){
            try {
                if (numeroFase == MULTIPLAYER1) {
                    arrayPlayers[0].X = 60; arrayPlayers[0].Y = 40;
                    arrayPlayers[1].X = 60; arrayPlayers[1].Y = 540;
                    arrayPlayers[2].X = 860; arrayPlayers[2].Y = 540;
                    arrayPlayers[3].X = 860; arrayPlayers[3].Y = 40;   
                    try {
                        imagensAmbiente[FUNDO] = new ImageIcon(getClass().getResource("Resources/FaseMultiplayer/chao1.png")).getImage();
                        imagensAmbiente[BLOCO] = new ImageIcon("Resources/FaseMultiplayer/blocoFixo.png").getImage();
                        imagensAmbiente[BLOCOQUEBRAVEL] = (new ImageIcon("Resources/FaseMultiplayer/blocosQuebraveis.png")).getImage();
                        imagensAmbiente[MARGEM_CE] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemCimaEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_C] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemCima.png").getImage();
                        imagensAmbiente[MARGEM_CD] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemCimaDireita.png").getImage();
                        imagensAmbiente[MARGEM_E] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_D] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemDireita.png").getImage();
                        imagensAmbiente[MARGEM_BE] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemBaixoEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_B] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemBaixo.png").getImage();
                        imagensAmbiente[MARGEM_BD] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemBaixoDireita.png").getImage();
                        doorClosed = new ImageIcon("Resources//FaseMultiplayer//doorClosed.png").getImage();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "A imagem não pode ser carregada! (Multiplayer)\n" + e, "Erro", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                    funcAdcBlocosFixos(MULTIPLAYER1);
                    funcAdcBlocosQuebraveis(MULTIPLAYER1);
                    funcAdcItens(1, 2, 2, 1);
                    funcAdcInimigos(MULTIPLAYER1);
                } else if (numeroFase == MULTIPLAYER2) {
                    try {
                        imagensAmbiente[FUNDO] = new ImageIcon("Resources/Fase1/chao1.png").getImage();
                        imagensAmbiente[BLOCO] = new ImageIcon("Resources/Fase1/blocoFixo.png").getImage();
                        imagensAmbiente[BLOCOQUEBRAVEL] = (new ImageIcon("Resources/Fase1/blocosQuebraveis.png")).getImage();
                        imagensAmbiente[MARGEM_CE] = new ImageIcon("Resources/Fase1/blocoMargemCimaEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_C] = new ImageIcon("Resources/Fase1/blocoMargemCima.png").getImage();
                        imagensAmbiente[MARGEM_CD] = new ImageIcon("Resources/Fase1/blocoMargemCimaDireita.png").getImage();
                        imagensAmbiente[MARGEM_E] = new ImageIcon("Resources/Fase1/blocoMargemEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_D] = new ImageIcon("Resources/Fase1/blocoMargemDireita.png").getImage();
                        imagensAmbiente[MARGEM_BE] = new ImageIcon("Resources/Fase1/blocoMargemBaixoEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_B] = new ImageIcon("Resources/Fase1/blocoMargemBaixo.png").getImage();
                        imagensAmbiente[MARGEM_BD] = new ImageIcon("Resources/Fase1/blocoMargemBaixoDireita.png").getImage();
                        doorClosed = new ImageIcon("Resources//Fase1//doorClosed.png").getImage();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "A imagem não pode ser carregada! (Fase 1)\n" + e, "Erro", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                    funcAdcBlocosFixos(MULTIPLAYER2);
                    funcAdcBlocosQuebraveis(MULTIPLAYER2);
                    funcAdcItens(1, 2, 2, 1);
                    funcAdcInimigos(MULTIPLAYER2);
                } else if (numeroFase == MULTIPLAYER3) {
                    try {
                        imagensAmbiente[FUNDO] = new ImageIcon("Resources/Fase2/chao1.png").getImage();
                        imagensAmbiente[BLOCO] = new ImageIcon("Resources/Fase2/blocoFixo.png").getImage();
                        imagensAmbiente[BLOCOQUEBRAVEL] = (new ImageIcon("Resources/Fase2/blocosQuebraveis.png")).getImage();
                        imagensAmbiente[MARGEM_CE] = new ImageIcon("Resources/Fase2/blocoMargemCimaEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_C] = new ImageIcon("Resources/Fase2/blocoMargemCima.png").getImage();
                        imagensAmbiente[MARGEM_CD] = new ImageIcon("Resources/Fase2/blocoMargemCimaDireita.png").getImage();
                        imagensAmbiente[MARGEM_E] = new ImageIcon("Resources/Fase2/blocoMargemEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_D] = new ImageIcon("Resources/Fase2/blocoMargemDireita.png").getImage();
                        imagensAmbiente[MARGEM_BE] = new ImageIcon("Resources/Fase2/blocoMargemBaixoEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_B] = new ImageIcon("Resources/Fase2/blocoMargemBaixo.png").getImage();
                        imagensAmbiente[MARGEM_BD] = new ImageIcon("Resources/Fase2/blocoMargemBaixoDireita.png").getImage();
                        doorClosed = new ImageIcon("Resources//Fase2//doorClosed.png").getImage();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "A imagem não pode ser carregada! (Fase 2)\n" + e, "Erro", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                    funcAdcBlocosFixos(MULTIPLAYER3);
                    funcAdcBlocosQuebraveis(MULTIPLAYER3);
                    funcAdcItens(1, 1, 1, 1);
                    funcAdcInimigos(MULTIPLAYER3);
                }
                else if (numeroFase == MULTIPLAYER4){
                    try {
                        imagensAmbiente[FUNDO] = new ImageIcon("Resources/Fase3/chao1.png").getImage();
                        imagensAmbiente[BLOCO] = new ImageIcon("Resources/Fase3/blocoFixo.png").getImage();
                        imagensAmbiente[BLOCOQUEBRAVEL] = (new ImageIcon("Resources/Fase3/blocosQuebraveis.png")).getImage();
                        imagensAmbiente[MARGEM_CE] = new ImageIcon("Resources/Fase3/blocoMargemCimaEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_C] = new ImageIcon("Resources/Fase3/blocoMargemCima.png").getImage();
                        imagensAmbiente[MARGEM_CD] = new ImageIcon("Resources/Fase3/blocoMargemCimaDireita.png").getImage();
                        imagensAmbiente[MARGEM_E] = new ImageIcon("Resources/Fase3/blocoMargemEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_D] = new ImageIcon("Resources/Fase3/blocoMargemDireita.png").getImage();
                        imagensAmbiente[MARGEM_BE] = new ImageIcon("Resources/Fase3/blocoMargemBaixoEsquerda.png").getImage();
                        imagensAmbiente[MARGEM_B] = new ImageIcon("Resources/Fase3/blocoMargemBaixo.png").getImage();
                        imagensAmbiente[MARGEM_BD] = new ImageIcon("Resources/Fase3/blocoMargemBaixoDireita.png").getImage();
                        doorClosed = new ImageIcon("Resources//Fase3//doorClosed.png").getImage();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "A imagem não pode ser carregada! (Fase 3)\n" + e, "Erro", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                    funcAdcBlocosFixos(MULTIPLAYER4);
                    funcAdcBlocosQuebraveis(MULTIPLAYER4);
                    funcAdcItens(1, 1, 1, 1);
                    funcAdcInimigos(MULTIPLAYER4);
                }
            } catch (Exception e){
                System.out.println("Erro Construtor Fases: " + e);
            }
            setLayout(null);
            setPreferredSize(new Dimension(imagensAmbiente[FUNDO].getWidth(this), imagensAmbiente[FUNDO].getHeight(this)));
        }

        void funcAdcBlocosFixos(int fase){
            try {
                blocosFixos = new Rectangle[40];
                int i, j, index = 0;
                if (fase == MULTIPLAYER1) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                } else if (fase == MULTIPLAYER2) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                } else if (fase == MULTIPLAYER3) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                }
                else if (fase == MULTIPLAYER4) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                }
            }catch (Exception e){
                System.out.println("Erro Blocos Fixos: "+e);
            }
        }

        void funcAdcBlocosQuebraveis(int fase){ //Cria o array dos blocos quebráveis da Fase 1
            try {
                arrayBlocosQuebraveis = new ArrayList<>(60);
                int y;
                if (fase == MULTIPLAYER1) {
                    y = 50; //LINHA 1
                    arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                } else if (fase == MULTIPLAYER2) {
                    y = 50; //LINHA 1
                    arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                    y = 100; //LINHA 2
                    arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                    y = 150; //LINHA 3
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                    y = 200; //LINHA 4
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                    y = 250; //LINHA 5
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                    y = 300; //LINHA 6
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                    y = 350; //LINHA 7
                    arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                    y = 400; //LINHA 8
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                    y = 450; //LINHA 9
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                    y = 500; //LINHA 10
                    arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                    y = 550; //LINHA 11
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(300, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                } else if (fase == MULTIPLAYER3) {
                    y = 50; //LINHA 1
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                    y = 100; //LINHA 2
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                    y = 150; //LINHA 3
                    arrayBlocosQuebraveis.add(new Rectangle(100, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                    y = 200; //LINHA 4
                    arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                    y = 250; //LINHA 5
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                    y = 300; //LINHA 6
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                    y = 350; //LINHA 7
                    arrayBlocosQuebraveis.add(new Rectangle(100, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                    y = 400; //LINHA 8
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                    y = 450; //LINHA 9
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(300, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                    y = 500; //LINHA 10
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                    y = 550; //LINHA 11
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                }
                else if (fase == MULTIPLAYER4){
                    y = 50; //LINHA 1
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                    y = 100; //LINHA 2
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                    y = 150; //LINHA 3
                    arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                    y = 200; //LINHA 4
                    arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                    y = 250; //LINHA 5
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                    y = 300; //LINHA 6
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                    y = 350; //LINHA 7
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(100, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                    y = 400; //LINHA 8
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                    y = 450; //LINHA 9
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(300, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                    y = 500; //LINHA 10
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                    y = 550; //LINHA 11
                    arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                    arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                }
            }catch (Exception e){
                System.out.println("Erro Blocos Quebraveis: "+e);
            }
        }

        void funcAdcItens (int itemBota, int itemTamanhoExplosao, int itemQtdeBombas, int itemVida){
            try {
                arrayItens = new ArrayList<>(itemBota + itemTamanhoExplosao + itemQtdeBombas + itemVida);
                int i;
                int[] posicaoDosItens = new int[itemBota + itemTamanhoExplosao + itemQtdeBombas + itemVida]; //guarda a posição dos itens, tamanho do vetor é qtde de itens max
                Random numeroAleatorio = new Random();
                int indexPosicoes = 0, numeroAleatorioItemAux;

                for (i = 0; i < itemBota; i++) {
                    if(indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                        break;
                    do {
                        numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um numero aleatorio
                    } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                    arrayItens.add(new Itens(ITEM_BOTA, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(), (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                    posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                    indexPosicoes++;
                }

                for (i = 0; i < itemTamanhoExplosao; i++) {
                    if(indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                        break;
                    do {
                        numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um numero aleatorio
                    } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                    arrayItens.add(new Itens(ITEM_TAMANHOEXPLOSAO, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(), (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                    posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                    indexPosicoes++;
                }

                for (i = 0; i < itemQtdeBombas; i++) {
                    if(indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                        break;
                    do {
                        numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um numero aleatorio
                    } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                    arrayItens.add(new Itens(ITEM_QTDEBOMBAS, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(), (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                    posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                    indexPosicoes++;
                }

                for (i = 0; i < itemVida; i++) {
                    if(indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                        break;
                    do {
                        numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um numero aleatorio
                    } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                    arrayItens.add(new Itens(ITEM_VIDA, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(), (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                    posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                    indexPosicoes++;
                }
            }catch (Exception e){
                System.out.println("Erro Adc Itens: "+e);
            }
        }

        void funcAdcInimigos(int fase){
            try {
                arrayInimigos = new ArrayList<>(10);
                if (fase == MULTIPLAYER1) {

                } else if (fase == MULTIPLAYER2) {
                    arrayInimigos.add(new Inimigo(400, 50, 2, HORIZONTAL, morcegoDireita, morcegoEsquerda));
                    arrayInimigos.add(new Inimigo(850, 300, 2, VERTICAL, morcegoCima, morcegoBaixo));
                    arrayInimigos.add(new Inimigo(250, 250, 2, HORIZONTAL, magoDireita, magoEsquerda));
                    arrayInimigos.add(new Inimigo(250, 50, 1, HORIZONTAL, cavaleiroDireita, cavaleiroEsquerda));
                    arrayInimigos.add(new Inimigo(750, 250, 2, VERTICAL, magoCima, magoBaixo));
                    arrayInimigos.add(new Inimigo(450, 250, 1, VERTICAL, cavaleiroCima, cavaleiroBaixo));
                } else if (fase == MULTIPLAYER3) {
                    arrayInimigos.add(new Inimigo(550, 550, 2, HORIZONTAL, bauDireita, bauEsquerda));
                    arrayInimigos.add(new Inimigo(250, 250, 2, VERTICAL, bauCima, bauBaixo));
                    arrayInimigos.add(new Inimigo(200, 50, 2, HORIZONTAL, jenovaDireita, jenovaEsquerda));
                    arrayInimigos.add(new Inimigo(550, 450, 2, VERTICAL, jenovaCima, jenovaBaixo));
                    arrayInimigos.add(new Inimigo(150, 150, 2, HORIZONTAL, bruxaDireita, bruxaEsquerda));
                    arrayInimigos.add(new Inimigo(650, 150, 2, VERTICAL, bruxaCima, bruxaBaixo));
                }
                else if (fase == MULTIPLAYER4){
                    arrayInimigos.add(new Inimigo(300, 550, 2, HORIZONTAL, verdeDireita, verdeEsquerda));
                    arrayInimigos.add(new Inimigo(650, 250, 2, VERTICAL, verdeCima, verdeBaixo));
                    arrayInimigos.add(new Inimigo(200, 450, 2, HORIZONTAL, elfoDireita, elfoEsquerda));
                    arrayInimigos.add(new Inimigo(550, 450, 2, VERTICAL, elfoCima, elfoBaixo));
                    arrayInimigos.add(new Inimigo(150, 150, 2, HORIZONTAL, andarilhoDireita, andarilhoEsquerda));
                    arrayInimigos.add(new Inimigo(850, 150, 2, VERTICAL, andarilhoCima, andarilhoBaixo));
                }
            }catch (Exception e){
                System.out.println("Erro Adc Inimigos: "+e);
            }
        }

        boolean funcChecaPosItens(int valor, int[] arrayDosItens){
            for (int arrayDosIten : arrayDosItens) {
                if (valor == arrayDosIten) {
                    return true;
                }
            }
            return false;
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            int i;
            boolean boolInimigoRemovido = false;
            try{
                /// Desenha o fundo
                g.drawImage(imagensAmbiente[FUNDO], 0, 0, getSize().width, getSize().height, this);
                System.out.println("1");
                /// Desenha as bordas superior e inferior
                for(i=50; i <= imagensAmbiente[FUNDO].getWidth(this)-100; i+=50){
                    g.drawImage(imagensAmbiente[MARGEM_C], i, 0,50,50, this);
                    System.out.println("2");
                    g.drawImage(imagensAmbiente[MARGEM_B], i, imagensAmbiente[FUNDO].getHeight(this)-50,50,50, this);
                    System.out.println("3");
                }
                /// Desenha as bordas da esquerda e da direita
                for(i=50; i <= imagensAmbiente[FUNDO].getHeight(this)-100; i+=50){
                    g.drawImage(imagensAmbiente[MARGEM_E], 0, i,50,50, this);
                    System.out.println("4");
                    g.drawImage(imagensAmbiente[MARGEM_D], imagensAmbiente[FUNDO].getWidth(this)-50, i,50,50, this);
                    System.out.println("5");
                }
                /// Desenha as quinas
                g.drawImage(imagensAmbiente[MARGEM_CE], 0, 0,50,50, this);
                System.out.println("6");
                g.drawImage(imagensAmbiente[MARGEM_CD], 900, 0,50,50, this);
                System.out.println("7");
                g.drawImage(imagensAmbiente[MARGEM_BE], 0, 600,50,50, this);
                System.out.println("8");
                g.drawImage(imagensAmbiente[MARGEM_BD], 900, 600,50,50, this);
                System.out.println("9");

                /// Coloca os blocos fixos dentro do campo de jogo
                for(int y=100; y<=BLOCOSVERTICAIS*100; y+=100){
                    for(i=100; i<=BLOCOSHORIZONTAIS*100; i+=100){
                        g.drawImage(imagensAmbiente[BLOCO], i, y, this);
                        System.out.println("10");
                    }
                }
                /// Desenha os itens
                for (i = 0; i < arrayItens.size(); i++) {
                    g.drawImage(arrayItens.get(i).imagensItemAnimacao[indexItems].getImage(), arrayItens.get(i).posX, arrayItens.get(i).posY, 40, 40, this);
                    System.out.println("11");
                    if (arrayItens.isEmpty())
                        break;
                }
                /// Coloca os blocos quebráveis dentro do campo de jogo
                for(i=0 ; i<arrayBlocosQuebraveis.size(); i++) {
                    g.drawImage(imagensAmbiente[BLOCOQUEBRAVEL], (int)arrayBlocosQuebraveis.get(i).getX(), (int)arrayBlocosQuebraveis.get(i).getY(), this);
                    System.out.println("12");
                    if(arrayBlocosQuebraveis.isEmpty())
                        break;
                }
                /// DESENHA AS BOMBAS
                if(arrayBombas.size()>0){
                    for(i = 0; i < arrayBombas.size(); i++){
                        if(arrayBombas.get(i).indexImage==99) {
                            funcExplodeBomba(arrayBombas.get(i));
                            arrayBombas.remove(arrayBombas.get(i));
                            if(arrayBombas.isEmpty())
                                break;
                        }
                        g.drawImage(arrayBombas.get(i).arrayImagensBomba[arrayBombas.get(i).indexImage/5], arrayBombas.get(i).getX()+13, arrayBombas.get(i).getY()+13, 25 ,25, this);
                    }
                }
                /// DESENHA AS EXPLOSOES
                if(arrayExplosao.size()>0){
                    for(i = 0; i < arrayExplosao.size(); i++){
                        if (arrayExplosao.get(i).tipoDeAnimacao==0){
                            g.drawImage(explosao, arrayExplosao.get(i).x, arrayExplosao.get(i).y, this);
                        }
                        else{
                            g.drawImage(explosao2, arrayExplosao.get(i).x, arrayExplosao.get(i).y, this);
                        }

                        //Checa colisao explosao com outra bomba
                        for(int j=0; j < arrayBombas.size();j++){
                            if(arrayExplosao.get(i).hitBox.intersects(arrayBombas.get(j).getHitBox())){
                                funcExplodeBomba(arrayBombas.get(j));
                                //sem a melhoria de remover apenas o ultimo
                                arrayBombas.remove(j);
                            }
                            if (arrayBombas.isEmpty())
                                break;
                        }
                        // Checa colisao da explosao com o player
                       // if(!currentPlayer.boolDanoRecente && arrayExplosao.get(i).hitBox.intersects(currentPlayer.getHitBox())) {
                          //  currentPlayer.danificado();
                       // }
                        if(multiplay){
                            for (i = 0; i < 4; i++){
                                if (arrayPlayers[i] != null && !arrayPlayers[i].boolDanoRecente && arrayExplosao.get(i).hitBox.intersects(arrayPlayers[i].getHitBox())){
                                    arrayPlayers[i].danificado();
                                    System.out.println("dano player " + i);
                                }
                            }
                           /* if(player2 != null && !player2.boolDanoRecente && arrayExplosao.get(i).hitBox.intersects(player2.getHitBox())) {
                                player2.danificado();
                                System.out.println("dano player2");
                            }
                            if(player3 != null && !player3.boolDanoRecente && arrayExplosao.get(i).hitBox.intersects(player3.getHitBox())) {
                                player3.danificado();
                            }
                            if(player4 != null && !player4.boolDanoRecente && arrayExplosao.get(i).hitBox.intersects(player4.getHitBox())) {
                                player4.danificado();
                            } */
                        }
                        /// Checa Colisao da explosao com o inimigo
                    /*    for (int j=0; j<arrayInimigos.size();j++) {
                            if (arrayExplosao.get(i).hitBox.intersects(arrayInimigos.get(j).getBounds()) && arrayExplosao.get(i).holdDraw > 0){ // Checa colisao da bomba com o inimigo
                                arrayInimigos.remove(j); // Remove o objeto inimigo do arrayInimigos (não será mais desenhado)
                                //barraSuperior.scoreMonstro+=100;
                                somaScore+=100;
                                boolInimigoRemovido = true;
                            } 
                            if(arrayInimigos.isEmpty()) {
                                break;
                            }
                            else if (boolInimigoRemovido){
                                j=0;
                                boolInimigoRemovido = false;
                            }
                        }  */
                        arrayExplosao.get(i).holdDraw--;
                        if(arrayExplosao.get(i).holdDraw<0){ //Checa a colisao da explosão com os blocos quebraveis
                            for(int j=0; j<arrayBlocosQuebraveis.size();j++){
                                if((arrayExplosao.get(i).hitBox.intersects(arrayBlocosQuebraveis.get(j)))){
                                    arrayBlocosQuebraveis.remove(j); // Remove os blocos quebraveis
                                    somaScore+=25;
                                    labelScore.setText(String.valueOf(somaScore));
                                }
                            }
                            ////////////////////////////////////remoçao de itens faltando
                            arrayExplosao.remove(i);
                        }
                    }
                }

                /// Desenha o Inimigo

               /* if (fase.arrayInimigos.size()>0) {
                    for (i = 0; i < arrayInimigos.size(); i++) {
                        if(arrayInimigos.get(i)!=null)
                            g.drawImage(arrayInimigos.get(i).getImage(), arrayInimigos.get(i).x+8, arrayInimigos.get(i).y-5, 31, 47, this);
                        if(arrayInimigos.isEmpty()){
                            break;
                        }
                    }
                } */
                /// Desenha o player1 (posicao inicial 40x40)
               // g.drawImage(arrayPlayers[0].personagem, arrayPlayers[0].getX(), arrayPlayers[0].getY(), 30,50,this);
                
               
               for (i = 0; i < 4; i++){
                    if (arrayPlayers[i] != null){
                        g.drawImage(arrayPlayers[i].personagem, arrayPlayers[i].getX(), arrayPlayers[i].getY(), 30, 50, this);
                        System.out.println("16");
                    }
                }

                
                /// Demais players
               // if(arrayPlayers[1] != null)
                   // g.drawImage(arrayPlayers[1].personagem, arrayPlayers[1].getX(), arrayPlayers[1].getY(), 30,50,this);
                //if(player3 != null)
                  //  g.drawImage(player3.personagem, player3.getX(), player3.getY(), 30,50,this);
                //if(player4 != null)
                 //   g.drawImage(player4.personagem, player4.getX(), player4.getY(), 30,50,this);

                // Condições para o game over
                if(single) { // Se for single player
                   // if (currentPlayer.getVida() == 0 || barraSuperior.valorTempo <= 0) { // Condicao para morrer
                       // boolGameOver = true;
                        //g.drawImage(gameOver, 0, 0, this);
                    }
               // }
                else { // Se for multiplayer
                    if(barraSuperior.valorTempo <= 0){
                        System.out.println("EMPATE?");
                    }else {
                        for (i = 0; i < 4; i++){
                            if (arrayPlayers[i]!=null)
                                if (arrayPlayers[i].getVida() <=0 ){
                                    System.out.println("Player " + i + "morto");
                                }
                        }
                       /* if (player1.getVida() <= 0) { // Condicao para morrer
                            System.out.println("Player1 morto");
                        }
                        if (player2.getVida() <= 0) { // Condicao para morrer
                            System.out.println("Player2 morto");
                        }
                        if (player3.getVida() <= 0) { // Condicao para morrer
                            System.out.println("Player3 morto");
                        }
                        if (player4.getVida() <= 0) { // Condicao para morrer
                            System.out.println("Player4 morto");
                        } */
                    }
                }
                /// Passar de fase
             /*   if (!multiplay && arrayInimigos.size() == 0) {
                    if (!ultimaFase) {
                        g.drawImage(fase.doorClosed, 450, 300, 50, 50, this);
                        if (player1.getHitBox().intersects(colisaoPorta)) {
                            passarFase = true;
                            g.setFont(fonte);
                            g.setColor(Color.white);
                            g.drawString("Pressione Enter para continuar", 253, 335);
                        } else {
                            passarFase = false;
                        }
                    }
                    else {
                        if (arrayInimigos.size() == 0){
                            g.setFont(fonte);
                            g.setColor(Color.white);
                            g.drawString("Pressione Enter para finalizar", 253, 335);
                            passarFase = false;
                            endGame = 0;
                        }
                    }
                } */

                Toolkit.getDefaultToolkit().sync();
            } catch (Exception e){
                System.out.println("Erro draw: "+ e);
            }
        }

    }

    void vida (){
        primeiroCoracao.setIcon(coracaoCheio);
        segundoCoracao.setIcon(coracaoCheio);
        terceiroCoracao.setIcon(coracaoCheio);
    }

    public void checkVida(){
        if (arrayPlayers[0].getVida() == 3){
            primeiroCoracao.setIcon(coracaoCheio);
            segundoCoracao.setIcon(coracaoCheio);
            terceiroCoracao.setIcon(coracaoCheio);
        } else if (arrayPlayers[0].getVida() == 2) {
            primeiroCoracao.setIcon(coracaoVazio);
            segundoCoracao.setIcon(coracaoCheio);
            terceiroCoracao.setIcon(coracaoCheio);
        }
        else if (arrayPlayers[0].getVida() == 1){
            primeiroCoracao.setIcon(coracaoVazio);
            segundoCoracao.setIcon(coracaoVazio);
            terceiroCoracao.setIcon(coracaoCheio);
        } else if (arrayPlayers[0].getVida() == 0){
            primeiroCoracao.setIcon(coracaoVazio);
            segundoCoracao.setIcon(coracaoVazio);
            terceiroCoracao.setIcon(coracaoVazio);
            boolGameOver = true;
        }
        repaint();
    }

    boolean intersBombas(Rectangle checkR){
        for (Bomba bombaBlock : arrayBombas) {
            if (bombaBlock.boolBloqueandoPlayer && checkR.intersects(bombaBlock.getHitBox()))
                return false;
        }
        return true;
    }

    boolean intersBombasInimigos(Rectangle checkR){
        for (Bomba bombaBlock : arrayBombas) {
            if (bombaBlock.boolBloqueandoInimigo && checkR.intersects(bombaBlock.getHitBox()))
                return false;
        }
        return true;
    }

    boolean intersBlocosFixos(Rectangle checkR, Rectangle[] blocosFixos){ //Retorna TRUE se não há colisão
        for(int i=0;i<40;i++){
            if(checkR.intersects(blocosFixos[i])){
                return false;
            }
        }
        return true;
    }

    boolean intersItem (Rectangle checkR){ //Retorna TRUE se não há colisão
        for (Itens c : arrayItens){
            if (checkR.intersects(c.getBounds())){
                return false;
            }
        }
        return true;
    }

    boolean intersBlocosQuebraveis(Rectangle checkR, ArrayList<Rectangle> arrayBlocosQuebraveis){ //Retorna TRUE se não há colisão
        for (Rectangle arrayBlocosQuebravei : arrayBlocosQuebraveis) {
            if (checkR.intersects(arrayBlocosQuebravei)) {
                return false;
            }
        }
        return true;
    }

    boolean inimigoIntersX(Rectangle checkR, ArrayList<Rectangle> arrayBlocosQuebraveis){ //Retorna TRUE se não há colisão
        for (Rectangle arrayBlocosQuebravei : arrayBlocosQuebraveis) {
            if (arrayBlocosQuebravei.getY() == checkR.getY()) {
                if (arrayBlocosQuebravei.intersects(checkR) && arrayBlocosQuebravei.intersects(checkR)) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean inimigoIntersY(Rectangle checkR,  ArrayList<Rectangle> arrayBlocosQuebraveis){ //Retorna TRUE se não há colisão
        for (Rectangle arrayBlocosQuebravei : arrayBlocosQuebraveis) {
            if (arrayBlocosQuebravei.getX() == checkR.getX()) {
                if (arrayBlocosQuebravei.intersects(checkR) && arrayBlocosQuebravei.intersects(checkR)) {
                    return false;
                }
            }
        }
        return true;
    }

    void funcExplodeBomba(Bomba bomba){
        int expX = bomba.getX(), expY = bomba.getY();
        arrayExplosao.add(new pontoBomba(expX, expY, 0));
        for(int i = 0, fogoSobe = expY-50; fogoSobe>=50 && i<bomba.valorBombaSize && intersBlocosFixos(new Rectangle(expX, fogoSobe,50,50), fase.blocosFixos); fogoSobe-=50, i++){
            if(intersBlocosQuebraveis((new Rectangle(expX,fogoSobe,50,50)), fase.arrayBlocosQuebraveis)){
                arrayExplosao.add(new pontoBomba(expX, fogoSobe, 0));
            } else {
                arrayExplosao.add(new pontoBomba(expX, fogoSobe, 1));
                break;
            }
        }
        for(int i = 0, fogoDesce = expY+50; fogoDesce<=550 && i<bomba.valorBombaSize && intersBlocosFixos(new Rectangle(expX, fogoDesce,50,50), fase.blocosFixos); fogoDesce+=50, i++){
            if(intersBlocosQuebraveis((new Rectangle(expX,fogoDesce,50,50)), fase.arrayBlocosQuebraveis)){
                arrayExplosao.add(new pontoBomba(expX, fogoDesce, 0));
            } else {
                arrayExplosao.add(new pontoBomba(expX, fogoDesce, 1));
                break;
            }
        }
        for(int i = 0, fogoEsquerda = expX-50; fogoEsquerda>=50 && i<bomba.valorBombaSize && intersBlocosFixos(new Rectangle(fogoEsquerda, expY,50,50), fase.blocosFixos); fogoEsquerda-=50, i++){
            if(intersBlocosQuebraveis((new Rectangle(fogoEsquerda,expY,50,50)), fase.arrayBlocosQuebraveis)){
                arrayExplosao.add(new pontoBomba(fogoEsquerda, expY, 0));
            } else {
                arrayExplosao.add(new pontoBomba(fogoEsquerda, expY, 1));
                break;
            }
        }
        for(int i = 0, fogoDireita = expX+50; fogoDireita<=850 && i<bomba.valorBombaSize && intersBlocosFixos(new Rectangle(fogoDireita, expY,50,50), fase.blocosFixos); fogoDireita+=50, i++){
            if(intersBlocosQuebraveis((new Rectangle(fogoDireita,expY,50,50)), fase.arrayBlocosQuebraveis)){
                arrayExplosao.add(new pontoBomba(fogoDireita, expY, 0));
            } else {
                arrayExplosao.add(new pontoBomba(fogoDireita, expY, 1));
                break;
            }
        }
    }

    static class pontoBomba extends Point{
        int holdDraw; //int que será decrementado durante a chamada Draw pra garantir com que a imagem seja desenhada holdDraw vezes
        int tipoDeAnimacao; //será 0 para a explosão normal e 1 para explosões que sobrepuserem os blocos quebráveis
        Rectangle hitBox;
        pontoBomba(int x, int y, int tipo){
            this.x=x;
            this.y=y;
            hitBox = new Rectangle(x,y,50,50);
            holdDraw=30;
            tipoDeAnimacao = tipo;
        }
    }

    class Bomba{
        int x, y, valorBombaSize,indexImage=0;
        Image[] arrayImagensBomba;
        Rectangle hitBox;
        boolean boolBloqueandoPlayer = false, boolBloqueandoInimigo = false;
        Bomba(int x, int y, int bombaSize){
            carregaImagens();
            valorBombaSize = bombaSize;
            //Setar o bombaY no centro dos espaços
            if(y<310){ //Divisão do Y na metade para possivelmente acelerar a chegada do if correspondente, como uma busca binária no início
                if(y>=25 && y<=60)
                    this.y = 50;
                else if (y<=110)
                    this.y = 100;
                else if (y<=160)
                    this.y = 150;
                else if (y<=210)
                    this.y = 200;
                else if (y<=260)
                    this.y = 250;
                else
                    this.y = 300;
            }
            else {
                if (y<=360)
                    this.y = 350;
                else if (y<=410)
                    this.y = 400;
                else if (y<=460)
                    this.y = 450;
                else if (y<=510)
                    this.y = 500;
                else if (y<=560)
                    this.y = 550;
            }
            //Setar o bombaX no centro dos espaços
            if(x<=535){ //Divisão do X na metade para possivelmente acelerar a chegada do if correspondente, como uma busca binária no início
                if(x<=85)
                    this.x = 50;
                else if (x<=135)
                    this.x = 100;
                else if (x<=185)
                    this.x = 150;
                else if (x<=235)
                    this.x = 200;
                else if (x<=285)
                    this.x = 250;
                else if (x<=335)
                    this.x = 300;
                else if (x<=385)
                    this.x = 350;
                else if (x<=435)
                    this.x = 400;
                else if (x<=485)
                    this.x = 450;
                else
                    this.x = 500;
            }
            else {
                if (x<=585)
                    this.x = 550;
                else if (x<=635)
                    this.x = 600;
                else if (x <= 685)
                    this.x = 650;
                else if (x<=735)
                    this.x = 700;
                else if (x<=785)
                    this.x = 750;
                else if (x<=835)
                    this.x = 800;
                else
                    this.x = 850;
            }
            hitBox = new Rectangle(this.x+12,this.y+12,26,26);
            boolLastBombaBlockPlayer = boolLastBombaBlockInimigo = true;
        }

        public Rectangle getHitBox(){
            return new Rectangle(this.x, this.y, 50,50);
        }

        public int getX(){
            return this.x;
        }

        public int getY(){
            return this.y;
        }

        void carregaImagens(){
            try {
                arrayImagensBomba = new Image[21];
                for(int i=0;i<20;i++){
                    arrayImagensBomba[i]= (new ImageIcon("Resources/Frames/bomb"+i+".png").getImage());
                }
            } catch (Exception b){
                System.out.println("Erro bomba: "+b);
            }
        }
    }

    class Player{
        int estado = PARADO, X = 60, Y = 40, maxBombas = 2, bombaSize = 1; //bombaSize = tamanho da bomba do player
        Image[] imagensPlayer = new Image[LENGTH_IMAGENS_PLAYER];
        Image personagem;
        int vida = 3, danoRecente = 0, velocidade = 4, qtdeItemBota, qtdeItemBomba, qtdeItemExplosao;
        boolean boolDanoRecente = false, boolStunned = false, moveRight = false, moveLeft = false, moveDown = false, moveUp = false;

        Player(int tipoPersonagem){
            try{
                imagensPlayer[PARADO] = new ImageIcon("Resources//Models//playerModel"+tipoPersonagem+"Parado.png").getImage();
                imagensPlayer[ANDANDO_DIREITA] = new ImageIcon("Resources//Models//playerModel"+tipoPersonagem+"Dir.gif").getImage();
                imagensPlayer[ANDANDO_ESQUERDA] = new ImageIcon("Resources//Models//playerModel"+tipoPersonagem+"Esq.gif").getImage();
                imagensPlayer[ANDANDO_FRENTE] = new ImageIcon("Resources//Models//playerModel"+tipoPersonagem+"Frente.gif").getImage();
                imagensPlayer[ANDANDO_COSTAS] = new ImageIcon("Resources//Models//playerModel"+tipoPersonagem+"Costas.gif").getImage();
                imagensPlayer[DANIFICADO] = new ImageIcon("Resources//Models//playerModel"+tipoPersonagem+"Damaged.gif").getImage();

                personagem = imagensPlayer[PARADO];
            }catch (Exception erroPlayer){
                System.out.println("Erro (Player): "+erroPlayer);
            }
            qtdeItemBomba = qtdeItemBota = qtdeItemExplosao = 0;
        }
        void danificado(){
            this.vida--;
            this.boolDanoRecente = true;
            this.danoRecente = 0;

            if(this.velocidade>4) {
                this.velocidade--;
                this.qtdeItemBota--;
                somaScore-=100;
                labelScore.setText(String.valueOf(somaScore));
                labelQuantidadeItemBota.setText("x"+arrayPlayers[0].qtdeItemBota);
            }
            if(this.bombaSize>1) {
                this.bombaSize--;
                this.qtdeItemExplosao--;
                somaScore-=100;
                labelScore.setText(String.valueOf(somaScore));
                labelQuantidadeItemExplosao.setText("x"+arrayPlayers[0].qtdeItemExplosao);
            }
            if(this.maxBombas > 2) {
                this.maxBombas--;
                this.qtdeItemBomba--;
                somaScore-=100;
                labelScore.setText(String.valueOf(somaScore));
                labelQuantidadeItemBomba.setText("x"+arrayPlayers[0].qtdeItemBomba);
            }
        }

        Rectangle getHitBox(){ //hitbox do player
            return new Rectangle(this.X,this.Y+15,30,35);
        }

        public int getX(){
            return this.X;
        }
        public int getY(){
            return this.Y;
        }
        public int getVida (){
            return vida;
        }
    }

    class Inimigo {
        final int HORIZONTAL = 0;
        int x,y,sentido;
        Image inimigo;
        ImageIcon direita;
        ImageIcon esquerda;
        ImageIcon cima;
        ImageIcon baixo;
        int velocidade;
        boolean inverteMovimento = false;

        Inimigo (int xInicial, int yInicial, int valorVelocidade, int valorSentido, String gifInimigo1, String gifInimigo2) {
            x=xInicial;
            y=yInicial;
            if (valorSentido == HORIZONTAL){
                direita = new ImageIcon("Resources//Enemies//" + gifInimigo1 + ".gif");
                esquerda = new ImageIcon("Resources//Enemies//"+ gifInimigo2 + ".gif");
            }
            else if (valorSentido == VERTICAL){
                cima = new ImageIcon("Resources//Enemies//" + gifInimigo1 + ".gif");
                baixo = new ImageIcon("Resources//Enemies//"+ gifInimigo2 + ".gif");
            }
            else if (valorSentido == HORIZONTAL_VERTICAL){
                direita = new ImageIcon("Resources//Enemies//" + gifInimigo1 + "Direita" + ".gif");
                esquerda = new ImageIcon("Resources//Enemies//"+ gifInimigo2 + "Esquerda" + ".gif");
                cima = new ImageIcon("Resources//Enemies//" + gifInimigo1 + "Cima" + ".gif");
                baixo = new ImageIcon("Resources//Enemies//"+ gifInimigo2 + "Baixo" + ".gif");
            }
            this.velocidade = valorVelocidade;
            this.sentido = valorSentido;
        }

        void move(){
            int XdaBomba;
            if (sentido == HORIZONTAL) {
                // DIREITA, INVERTEMOVIMENTO = FALSE
                if (x <= 850 && !inverteMovimento && inimigoIntersX(new Rectangle(x + velocidade, y, 40, 40), fase.arrayBlocosQuebraveis) && intersBombasInimigos(new Rectangle(x + velocidade, y, 40, 40))) {
                    inimigo = direita.getImage();
                    x += velocidade;
                }
                else {
                    inverteMovimento = true;
                }
                // ESQUERDA, INVERTEMOVIMENTO = TRUE
                if (x >= 50 && inverteMovimento && inimigoIntersX(new Rectangle(x - velocidade, y, 40, 40), fase.arrayBlocosQuebraveis) && intersBombasInimigos(new Rectangle(x - velocidade, y, 40, 40))) {
                    inimigo = esquerda.getImage();
                    x -= velocidade;
                }
                else {
                    inverteMovimento = false;
                }
            } else { // SENTIDO VERTICAL
                //PARA BAIXO, INVERTEMOVIMENTO = FALSO
                if (y <= 550 && !inverteMovimento && inimigoIntersY(new Rectangle(x , y + velocidade, 40, 40), fase.arrayBlocosQuebraveis) && intersBombasInimigos(new Rectangle(x, y + velocidade, 40, 40))) {
                    inimigo = baixo.getImage();
                    y += velocidade;
                }
                else {
                    inverteMovimento = true;
                }
                //PARA CIMA, INVERTEMOVIMENTO = TRUE
                if (y >= 50 && inverteMovimento && inimigoIntersY(new Rectangle(x , y - velocidade, 40, 40), fase.arrayBlocosQuebraveis) && intersBombasInimigos(new Rectangle(x , y - velocidade, 40, 40))) {
                    inimigo = cima.getImage();
                    y -= velocidade;
                }
                else {
                    inverteMovimento = false;
                }
            }
        }
        public Image getImage (){
            return inimigo;
        }
        public Rectangle getBounds(){
            return new Rectangle(x,y,31,47);
        }
    }

    class Itens {
        ImageIcon[] imagensItemAnimacao = new ImageIcon[7];
        Image imagem;
        boolean boolItemRecemQueimado = false;
        int item, recemQueimado = 0;
        int itemQueimado = 2; // quando a bomba queimar o item que estiver descoberto ele some (1- aparece 0-queima)
        int posX, posY;
        Itens (int tipoItem, int posX, int posY){
            int i;
            if(tipoItem==0) {
                for(i=0;i<7;i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/BotaIMG"+i+".png");
            } else if (tipoItem == 1){
                for(i=0;i<7;i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/TamanhoExplosaoIMG"+i+".png");
            } else if (tipoItem == 2){
                for(i=0;i<7;i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/QtdeBombasIMG"+i+".png");
            } else {
                for(i=0;i<7;i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/ItemVidaIMG"+i+".png");
            }
            item = tipoItem;
            this.posX = posX;
            this.posY = posY;
        }

        public Rectangle getBounds(){
            return new Rectangle(this.posX,this.posY,50,50);
        }

    }

    class layoutDeCima extends JPanel {
        int valorTempo = 150; //scoreMonstro=0;
        Color corVermelhoEscuro = new Color(145, 6, 13);
        Border blackline = BorderFactory.createLineBorder(Color.black);
        Image layoutBackGround = new ImageIcon(getClass().getResource("Resources/Layout/fundoBarraSuperior.png")).getImage();
        Timer tempo;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            GradientPaint gradient = new GradientPaint(0, 0, Color.black, 0, 40, corVermelhoEscuro);
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.drawImage(layoutBackGround,0,0,950,40,this);
        }

        layoutDeCima(){
            setBorder(blackline);
            Font font = new Font ("Georgia", Font.BOLD, 20);
            JPanel panelVidas = new JPanel(){
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Dimension arcs = new Dimension(30,30);
                    int width = getWidth();
                    int height = getHeight();
                    Graphics2D graphics = (Graphics2D) g;
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    //Draws the rounded opaque panel with borders.
                    graphics.setColor(getBackground());
                    graphics.fillRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint background
                    graphics.setColor(getForeground());
                    graphics.drawRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint border
                }
            };
            JPanel panelTempo = new JPanel(){
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Dimension arcs = new Dimension(30,30);
                    int width = getWidth();
                    int height = getHeight();
                    Graphics2D graphics = (Graphics2D) g;
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    //Draws the rounded opaque panel with borders.
                    graphics.setColor(getBackground());
                    graphics.fillRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint background
                    graphics.setColor(getForeground());
                    graphics.drawRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint border
                }
            };
            JPanel panelScore = new JPanel(){
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Dimension arcs = new Dimension(30,30);
                    int width = getWidth();
                    int height = getHeight();
                    Graphics2D graphics = (Graphics2D) g;
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    //Draws the rounded opaque panel with borders.
                    graphics.setColor(getBackground());
                    graphics.fillRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint background
                    graphics.setColor(getForeground());
                    graphics.drawRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint border
                }
            };

            labelTempoValue = new JLabel(String.valueOf(tempo));
            labelScore = new JLabel(String.valueOf(somaScore));
            // Define visual dos labels
            labelTempoValue.setFont(font);
            labelTempoValue.setForeground(Color.white);
            labelScore.setFont(font);
            labelScore.setForeground(Color.white);
            // Define visual dos paineis vida, tempo e score
            panelVidas.setOpaque(false);
            panelVidas.setBackground(Color.white);
            panelVidas.setForeground(Color.red);
            panelVidas.setBounds(76,3,100,34);
            panelTempo.setOpaque(false);
            panelTempo.setBackground(Color.black);
            panelTempo.setForeground(Color.red);
            panelTempo.setBounds(237,3,100,34);
            panelScore.setOpaque(false);
            panelScore.setBackground(Color.black);
            panelScore.setForeground(Color.red);
            panelScore.setBounds(393,3,100,34);
            // Atribui os labels aos respecivos paineis
            panelVidas.add(terceiroCoracao);
            panelVidas.add(segundoCoracao);
            panelVidas.add(primeiroCoracao);

            if (multiplay){
                panelVidas.add(terceiroCoracao);
                panelVidas.add(segundoCoracao);
                panelVidas.add(primeiroCoracao);
            }
            panelTempo.add(labelTempoValue);
            panelScore.add(labelScore);
            //Icons
            ImageIcon imgVidas = new ImageIcon(getClass().getResource("Resources/Layout/heart-beats.png"));
            JLabel labelImgVidas = new JLabel(imgVidas);
            labelImgVidas.setBounds(35,3,36,36);

            ImageIcon imgCronometro = new ImageIcon(getClass().getResource("Resources/Layout/stopwatch.png"));
            JLabel labelImgCronometro = new JLabel(imgCronometro);
            labelImgCronometro.setBounds(196,3,36,36);

            ImageIcon imgScore = new ImageIcon(getClass().getResource("Resources/Layout/score.png"));
            JLabel labelImgScore = new JLabel(imgScore);
            labelImgScore.setBounds(357,3,36,36);

            ImageIcon imgItemBota = new ImageIcon(getClass().getResource("Resources/Layout/itemLabelBota.png"));
            JLabel labelImgItemBota = new JLabel(imgItemBota);
            labelImgItemBota.setBounds(700,2,36,36);

            ImageIcon imgItemBomba = new ImageIcon(getClass().getResource("Resources/Layout/itemLabelBombas.png"));
            JLabel labelImgItemBomba = new JLabel(imgItemBomba);
            labelImgItemBomba.setBounds(760,2,36,36);

            ImageIcon imgItemExplosao = new ImageIcon(getClass().getResource("Resources/Layout/itemLabelExplosao.png"));
            JLabel labelImgItemExplosao = new JLabel(imgItemExplosao);
            labelImgItemExplosao.setBounds(820,2,36,36);

            //// Quantidade dos itens
            labelQuantidadeItemBota = new JLabel("x"+arrayPlayers[0].qtdeItemBota);
            labelQuantidadeItemBota.setBounds(737,10,36,36);
            labelQuantidadeItemBota.setForeground(Color.white);

            labelQuantidadeItemBomba = new JLabel("x"+arrayPlayers[0].qtdeItemBomba);
            labelQuantidadeItemBomba.setBounds(797,10,36,36);
            labelQuantidadeItemBomba.setForeground(Color.white);

            labelQuantidadeItemExplosao = new JLabel("x"+arrayPlayers[0].qtdeItemExplosao);
            labelQuantidadeItemExplosao.setBounds(857,10,36,36);
            labelQuantidadeItemExplosao.setForeground(Color.white);
            //
            setLayout(null);
            setBackground(new Color (120, 120, 120, 150));
            add(panelVidas);
            add(panelTempo);
            add(panelScore);
            add(labelImgVidas);
            add(labelImgCronometro);
            add(labelImgScore);
            add(labelImgItemBota);
            add(labelImgItemBomba);
            add(labelImgItemExplosao);
            add(labelQuantidadeItemBota);
            add(labelQuantidadeItemBomba);
            add(labelQuantidadeItemExplosao);
            setPreferredSize(new Dimension(950,40));

        }
    }

    class Menu extends JPanel  {
        String[] options = {"Jogar", "Multiplayer","Sobre" ,"Sair"};
        int currentOption = 0;
        int maxOption = options.length - 1;
        boolean cenarioSobre = false;
        Font font = new Font("Arial", Font.PLAIN, 20);
        Font fontText = new Font("Arial", Font.BOLD, 20);
        boolean ativo = true;
        ImageIcon patins = new ImageIcon("Resources//Itens//BotaIMG.png");
        ImageIcon itemVida = new ImageIcon("Resources//Itens//ItemVidaIMG.png");
        ImageIcon bomba = new ImageIcon("Resources//Itens//QtdeBombasIMG.png");
        ImageIcon explosao = new ImageIcon("Resources//Itens//TamanhoExplosaoIMG.png");
        ImageIcon fundo = new ImageIcon("Resources/Layout/BackGround.png");
        ImageIcon teclas = new ImageIcon("Resources//Itens//teclas.png");
        Image imagemTecla = teclas.getImage();
        Image p = patins.getImage();
        Image v = itemVida.getImage();
        Image b = bomba.getImage();
        Image e = explosao.getImage();
        Image back = fundo.getImage();
        Menu(){
            setPreferredSize(new Dimension(950,690));
        }
        int getCurrentOption (){
            return currentOption;
        }
        void tick(){
            if (currentOption < 0) {
                currentOption = 0;
            }
            if (currentOption > maxOption) {
                currentOption =  maxOption; // Colocar igual a maxOption se quiser travar
            }
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2d2 = (Graphics2D) g;
            GradientPaint degradeFundo = new GradientPaint(0, 0, new Color(184, 178, 178,100), 200, 0, new Color(255,255,255,150));
            GradientPaint degradeFundo2 = new GradientPaint(0, 0, new Color(184, 178, 178), 200, 0, new Color(255,255,255));
            g.drawImage(back,0,0,getWidth(),getHeight(),this);
            if (!cenarioSobre) {
                g.setFont(font);
                g.setColor(new Color(0,0,0));
                g.drawString("Jogar", 255, 352);
                g.drawString("Multiplayer",233 , 396);
                g.drawString("Sobre", 253, 440);
                g.drawString("Sair", 262, 484);
                if (currentOption == 0) {
                    g2d2.setPaint(degradeFundo);
                    g.fillRoundRect(184, 322, 192, 44, 0, 0);
                    g.setColor(Color.gray);
                    g.drawRoundRect(184, 322, 192, 44, 0, 0);
                    g.setColor(Color.black);
                    g.drawString("Jogar", 255, 352);

                } else if (currentOption == 1) {
                    g2d2.setPaint(degradeFundo);
                    g.fillRoundRect(184, 366, 192, 44, 0, 0);
                    g.setColor(Color.gray);
                    g.drawRoundRect(184, 366, 192, 44, 0, 0);
                    g.setColor(Color.black);
                    g.drawString("Multiplayer", 233, 396);
                } else if (currentOption == 2) {
                    g2d2.setPaint(degradeFundo);
                    g.fillRoundRect(184, 410, 192, 44, 0, 0);
                    g.setColor(Color.gray);
                    g.drawRoundRect(184, 410, 192, 44, 0, 0);
                    g.setColor(Color.black);
                    g.drawString("Sobre", 253, 440);
                }
                else if (currentOption == 3) {
                    g2d2.setPaint(degradeFundo);
                    g.fillRoundRect(184, 454, 192, 44, 0, 0);
                    g.setColor(Color.gray);
                    g.drawRoundRect(184, 454, 192, 44, 0, 0);
                    g.setColor(Color.black);
                    g.drawString("Sair", 262, 484);
                }

            }
            else if (cenarioSobre){
                g.setColor(new Color(0,0,0,200));
                g.fillRect(0, 0, getWidth(), getHeight());
                g2d2.setPaint(degradeFundo2);
                g.fillRoundRect(100,130,80,25,10,12);
                g.setColor(Color.white);
                g.setFont(fontText);
                g.drawImage(imagemTecla,100,50,80,60,this);
                g.drawString("Controla a movimentação do personagem", 220,90);
                g.drawRoundRect(100,130,80,25,10,12);
                g.drawString("Solta bomba", 220,150);
                g.drawImage(p,100,180,90,90,this);
                g.drawString("Aumenta a velocidade do personagem em 1", 220,225);
                g.drawImage(v,100,300,90,90,this);
                g.drawString("O personagem ganha 1 ponto de vida", 220,345);
                g.drawImage(b,100,430,90,90,this);
                g.drawString("Aumenta o limite de bombas", 220,475);
                g.drawImage(e,100,560,90,90,this);
                g.drawString("Aumenta o tamanho da explosão da bomba", 220,605);
            }
            repaint();
        }
        public void voltarAoMenu(KeyEvent e){
            if(e.getKeyCode() == e.VK_ESCAPE){
                cenarioSobre = false;
                ativo = true;
            }
        }

        private void controlarMenu(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_UP) {
                currentOption -= 1;
            }
            if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                currentOption+= 1;
            }
            tick();
            if(e.getKeyCode() == KeyEvent.VK_ENTER){
                ativo = false;
                if (currentOption == 2){
                    cenarioSobre = true;
                }
            }
        }
        public void controlar(KeyEvent e) {
            if (ativo) {
                controlarMenu(e);
            }
        }

    }

    public void carregaFase1(){
        try {
            if (gameControler != NAV_MENU) { //se vier da fase 2 ou outra
                remove(fase);
                barraSuperior.tempo.stop();
            } else {
                barraSuperior = new layoutDeCima();
                add(barraSuperior, BorderLayout.NORTH);
            }
            gameControler = NAV_FASE1;
            refreshModels.stop();
            fase = new Fase(MULTIPLAYER2);
            add(fase, BorderLayout.SOUTH);
            barraSuperior.valorTempo = TEMPO_DA_FASE;
            labelTempoValue.setText("150");
            barraSuperior.tempo = new Timer(1000, e -> {
                if (barraSuperior.valorTempo > 0) {
                    barraSuperior.valorTempo--;
                    labelTempoValue.setText(String.valueOf(barraSuperior.valorTempo));
                    labelScore.setText(String.valueOf(somaScore));
                }
            });
            barraSuperior.tempo.start();
            refreshModels.start();
            pack();
        }catch (Exception e){
            System.out.println("Erro carregaFase1: "+e);
        }
    }

    public void carregaFase2(){
        try {
            gameControler = NAV_FASE2;
            refreshModels.stop();
            remove(fase);
            fase = new Fase(MULTIPLAYER3);
            barraSuperior.tempo.stop();
            barraSuperior.valorTempo = TEMPO_DA_FASE;
            labelTempoValue.setText("150");
            barraSuperior.tempo = new Timer(1000, e -> {
                if (barraSuperior.valorTempo > 0) {
                    barraSuperior.valorTempo--;
                    labelTempoValue.setText(String.valueOf(barraSuperior.valorTempo));
                    labelScore.setText(String.valueOf(somaScore));
                }
            });
            add(fase, BorderLayout.SOUTH);
            barraSuperior.tempo.start();
            refreshModels.start();
            pack();
        }catch (Exception e){
            System.out.println("Erro carregaFase2: "+e);
        }
    }

    public void carregaFase3(){
        try {
            gameControler = NAV_FASE3;
            refreshModels.stop();
            remove(fase);
            fase = new Fase(MULTIPLAYER4);
            ultimaFase = true;
            barraSuperior.tempo.stop();
            barraSuperior.valorTempo = TEMPO_DA_FASE;
            labelTempoValue.setText("150");
            barraSuperior.tempo = new Timer(1000, e -> {
                if (barraSuperior.valorTempo > 0) {
                    barraSuperior.valorTempo--;
                    labelTempoValue.setText(String.valueOf(barraSuperior.valorTempo));
                    labelScore.setText(String.valueOf(somaScore));
                }
            });
            add(fase, BorderLayout.SOUTH);
            barraSuperior.tempo.start();
            refreshModels.start();
            pack();
        }catch (Exception e){
            System.out.println("Erro carregaFase2: "+e);
        }
    }

    public void salvarDadosPlayer (){
        while (!escreveu) {
            int op = JOptionPane.showConfirmDialog(this,  "Deseja salvar seu nome no ranking?", "Fim de jogo!", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                if (endGame == 0) {
                    nome_do_Player = JOptionPane.showInputDialog("Digite seu nome: ");
                    score_do_Player = Integer.toString(somaScore);
                    try {
                        FileWriter writer = new FileWriter(new File("Resources/Ranking/Ranking.txt"), true);
                        BufferedWriter conexao = new BufferedWriter(writer);
                        conexao.write(nome_do_Player + " ");
                        conexao.write(score_do_Player);
                        conexao.newLine();
                        conexao.close();
                        writer.close();
                        System.out.println("Arquivo criado com sucesso!");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    escreveu = true;
                }
            }
            else {
                escreveu = true;
            }
        }
    }

    public void carregaMultiplayer(){
        try {
            barraSuperior = new layoutDeCima();
            add(barraSuperior, BorderLayout.NORTH);
            gameControler = NAV_MULTIPLAYER;
            fase = new Fase(MULTIPLAYER1);
            barraSuperior.valorTempo = TEMPO_DA_FASE;
            labelTempoValue.setText("150");
            barraSuperior.tempo = new Timer(1000, e -> {
                if (barraSuperior.valorTempo > 0) {
                    barraSuperior.valorTempo--;
                    labelTempoValue.setText(String.valueOf(barraSuperior.valorTempo));
                    labelScore.setText(String.valueOf(somaScore));
                }
            });
            add(fase, BorderLayout.SOUTH);
            barraSuperior.tempo.start();
            refreshModels.start();
            pack();
        }catch (Exception e){
            System.out.println("Erro carregaMultiplayer: "+e);
        }
    }

    class Rede{
        Socket socket = null;
        DataInputStream is = null;
        DataOutputStream os = null;
        boolean temDados = true;
        Cliente jogo;
        public Rede(Cliente jogo, String IP, int porto){
            try {
                this.jogo = jogo;
                socket = new Socket(IP, porto);
                os = new DataOutputStream(socket.getOutputStream());
                is = new DataInputStream(socket.getInputStream());
            } catch (UnknownHostException e) {
                JOptionPane.showMessageDialog(jogo,"Servidor não encontrado!\n   " + e, "Erro", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(jogo, "Não pode trocar dados com o servidor!\n   " + e, "Erro",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        public void enviaPosicao(String tipo, int x, int y) {
            try {
                os.writeUTF(tipo);
                os.writeInt(x);
                os.writeInt(y);
            } catch (IOException e) {
                temDados = false;
            }
        }
        public void enviaBomba(String tipo, int x, int y){
            try {
                os.writeUTF(tipo);
                os.writeInt(x);
                os.writeInt(y);
            } catch (IOException e) {
                temDados = false;
            }
        }
        public boolean continua() {

            return temDados;
        }

        public String recebeMensagem (){
            try {
                return is.readUTF();
              } catch (IOException e) {
                temDados = false;
                return "";
              }
        }
        public void recebeVida(Player [] player) {
            try {
              for (int i = 0; i < 4; i++){
                    player[i].vida = is.readInt();
              }  
            } catch (IOException e) {
              temDados = false;
            }
          }

        public void recebePosicoesPlayers(Player[] player) {
            try {
                for (int i = 0; i < 4; i++) {
                    player[i].X = is.readInt();
                    player[i].Y = is.readInt();
                }
            } catch (IOException e) {
                temDados = false;
            }
        }

        public void recebePosicoesBombas(){ // 

        }
        public void descarregaEnvio() {
            try {
                os.flush();
            } catch (IOException e) {
                temDados = false;
            }
        }

    }

    Cliente(){
        super("Bomb Your Way Out");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.black);
        vida();
        gameControler = NAV_MENU;
        menu = new Menu();
        add(menu);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(gameControler == NAV_FASE1 || gameControler == NAV_FASE2 || gameControler == NAV_FASE3 || gameControler == NAV_MULTIPLAYER){
                    if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        arrayPlayers[0].moveRight = true;
                        rede.enviaPosicao("pos", arrayPlayers[0].getX(), arrayPlayers[0].getY());
                        rede.descarregaEnvio();
                    } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        arrayPlayers[0].moveLeft = true;
                        rede.enviaPosicao("pos", arrayPlayers[0].getX(), arrayPlayers[0].getY());
                        rede.descarregaEnvio();
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        arrayPlayers[0].moveDown = true;
                        rede.enviaPosicao("pos", arrayPlayers[0].getX(), arrayPlayers[0].getY());
                        rede.descarregaEnvio();
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        arrayPlayers[0].moveUp = true;
                        rede.enviaPosicao("pos", arrayPlayers[0].getX(), arrayPlayers[0].getY());
                        rede.descarregaEnvio();
                    } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        if(arrayBombas.size()<arrayPlayers[0].maxBombas){
                            arrayBombas.add(new Bomba(arrayPlayers[0].getX(), arrayPlayers[0].getY(), arrayPlayers[0].bombaSize));
                        }
                    }
                }
                else if (gameControler == NAV_MENU){
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        menu.controlar(e);
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        menu.controlar(e);
                    }
                }
            }
            @Override
            public void keyReleased(KeyEvent kr){
                if(gameControler == NAV_FASE1 || gameControler == NAV_FASE2 || gameControler == NAV_FASE3 || gameControler == NAV_MULTIPLAYER) {
                    if (kr.getKeyCode() == KeyEvent.VK_RIGHT) {
                        arrayPlayers[0].moveRight = false;
                        System.out.println("pos: "+arrayPlayers[0].getX()+","+ arrayPlayers[0].getY()+".");
                    } else if (kr.getKeyCode() == KeyEvent.VK_LEFT) {
                        arrayPlayers[0].moveLeft = false;
                        System.out.println("pos: "+arrayPlayers[0].getX()+","+ arrayPlayers[0].getY()+".");
                    }
                    if (kr.getKeyCode() == KeyEvent.VK_UP) {
                        arrayPlayers[0].moveUp = false;
                        System.out.println("pos: "+arrayPlayers[0].getX()+","+ arrayPlayers[0].getY()+".");
                    } else if (kr.getKeyCode() == KeyEvent.VK_DOWN) {
                        arrayPlayers[0].moveDown = false;
                        System.out.println("pos: "+arrayPlayers[0].getX()+","+ arrayPlayers[0].getY()+".");
                    } else if (kr.getKeyCode() == KeyEvent.VK_ENTER) {
                        if(gameControler == NAV_FASE1) {
                            if(passarFase) {
                                carregaFase2();
                                passarFase = false;
                            }
                        }
                        else if (gameControler == NAV_FASE2) {
                            if(passarFase) {
                                carregaFase3();
                                passarFase = false;
                            }
                        }
                        else if (gameControler == NAV_FASE3){
                            salvarDadosPlayer();
                            System.exit(1);
                        }
                    }
                }
                else if (gameControler == NAV_MENU){
                    if (kr.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (menu.getCurrentOption() == 0) {
                            remove(menu);
                            single = true;
                            carregaFase1();
                        } else if (menu.getCurrentOption() == 1) {
                            // MULTIPLAYER
                            remove(menu);
                            carregaMultiplayer();
                            multiplay = true;
                        } else if (menu.getCurrentOption() == 2) {
                            menu.cenarioSobre = true;
                        } else if (menu.getCurrentOption() == 3) {
                            System.exit(1);
                        }
                    } else if (kr.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        menu.voltarAoMenu(kr);
                    }
                }
            }

        });
        pack();
        setResizable(false);
        setVisible(true);
    }
    static public void main(String[] args) throws InterruptedException {
        new Cliente();
    }
}