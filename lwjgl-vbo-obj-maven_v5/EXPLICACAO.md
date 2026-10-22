# Como funciona a animação MD2 (guia de estudo)

Tudo em `src/main/java/aula/`. A ideia geral: `Md2Arquivo` lê o arquivo, `Animador` diz qual
quadro mostrar, `Md2Model` mistura dois quadros e desenha, `Main` junta tudo.

## 1. O que é animação por keyframe

Em vez de ter ossos, o modelo guarda várias **poses completas** (quadros-chave, ou keyframes):
em cada uma, todos os vértices têm uma posição. Animar é trocar de pose ao longo do tempo. O
Ogro tem 199 quadros e 358 vértices por quadro. Se só trocássemos de pose, o movimento seria
"truncado" (10 poses por segundo). Por isso **interpolamos**: entre a pose A e a pose B
calculamos poses intermediárias, e o movimento fica suave mesmo a 60 fps de tela.

## 2. O formato MD2

Tudo é little-endian. `Md2Arquivo` lê o arquivo inteiro para um `ByteBuffer` e vai pulando
para cada bloco usando os deslocamentos (offsets) do cabeçalho:

```
+-----------------------------+  posição 0
| cabeçalho (17 ints, 68 B)   |
+-----------------------------+  ofsSkins
| skins: N nomes de 64 bytes  |
+-----------------------------+  ofsSt
| st: numSt x (short s, t)    |  coordenadas de textura, em pixels
+-----------------------------+  ofsTris
| triângulos: numTriangulos x |  3 índices de vértice + 3 índices de st
+-----------------------------+  ofsQuadros
| quadros: numQuadros x       |  cada um com tamanhoQuadro bytes:
|   escala (3 floats)         |
|   translação (3 floats)     |
|   nome (16 bytes)           |
|   numVertices x (x,y,z,n)   |  4 bytes por vértice (n = normal, ignorada)
+-----------------------------+
```

(O arquivo ainda tem um bloco de comandos GL, depois dos quadros, que não usamos.)

Campos do cabeçalho, nesta ordem: `ident` ("IDP2"), versão (8), largura e altura da skin,
tamanho do quadro, nº de skins, nº de vértices, nº de st, nº de triângulos, nº de comandos GL,
nº de quadros, e os offsets de skins, st, triângulos, quadros, comandos GL e do fim. O
construtor valida `ident` e a versão e lança `IOException` com mensagem clara se não for MD2.

Como no OBJ, o triângulo guarda índices de vértice e de UV **separados** (`triVertices` e
`triUvs`).

## 3. Descompactação: `escala × byte + translação`

Cada coordenada de vértice ocupa **1 byte** (0 a 255) para o arquivo ser pequeno. Cada quadro
traz uma escala e uma translação que devolvem o valor real. No `Md2Arquivo`:

```java
int byteSemSinal = b.get(ofsVertice + eixo) & 0xFF;
posicoes[q][v * 3 + eixo] = escala[eixo] * byteSemSinal + translacao[eixo];
```

O `& 0xFF` é necessário porque `byte` em Java tem sinal (-128 a 127). Exemplo real, vértice 0
do quadro 0 do Ogro (`stand001`): bytes `95, 129, 105`, escala `(0.10477, 0.14705, 0.20507)` e
translação `(-9.3904, -17.5028, -24.2511)`:

- x = 0.10477 × 95 − 9.3904 = **0.562**
- y = 0.14705 × 129 − 17.5028 = **1.466**
- z = 0.20507 × 105 − 24.2511 = **−2.718**

É exatamente o que o teste `descompactaPosicoesDosQuadros` (`Md2ArquivoTest`) confere em
`posicoes[0][0..2]`. O resultado fica em `posicoes[quadro][vertice * 3 + eixo]`.

## 4. As 21 animações a partir dos nomes

O MD2 não tem "animações", só quadros com nomes (`stand001`, `stand002`, ..., `run001`, ...).
`Md2Animacao.montarLista` tira os dígitos do fim do nome (`replaceAll("\\d+$", "")`) e agrupa
quadros **vizinhos** com o mesmo prefixo: cada `Md2Animacao` guarda `nome`, `inicio` e `fim`
(inclusive). No Ogro dá 21: `stand` (0–39), `run` (40–45), `attack` (46–53), ..., `boom` (198).
A lista é montada em `Main` a partir dos nomes do corpo; a arma usa os mesmos índices.

## 5. A fórmula da interpolação

`Md2Model.interpolar`, para cada número do vértice:

```
resultado = A + (B − A) × t        (t entre 0 e 1)
```

t = 0 dá A, t = 1 dá B, t = 0.5 dá o ponto médio. Exemplo: se a coordenada x de um vértice
vale A = 1.0 no quadro 41 e B = 3.0 no quadro 42, com t = 0.35: 1.0 + (3.0 − 1.0) × 0.35 = **1.7**.
Isso é feito para os 358 × 3 números do quadro.

Com a interpolação **desligada** (tecla `I`), `Animador.fracao()` devolve sempre 0, então o
resultado é sempre A: o modelo "pula" de pose em pose, 10 vezes por segundo (na velocidade padrão). É assim que se vê
a diferença.

## 6. Tempo em keyframes

`Animador` guarda `tempo` em **keyframes**, dentro da animação atual. A cada volta do laço do
`Main` calcula-se `dt` (segundos desde o quadro anterior, via `glfwGetTime`) e:

```java
tempo = (tempo + dt * velocidade) % n;
```

- `velocidade` é em keyframes por segundo; o padrão é 10, o do Quake 2.
- `% n` (n = número de quadros da animação) faz o tempo dar a volta: todas as animações
  repetem em loop.
- Parte inteira do tempo = quadro de onde partimos (`quadroAtual`); parte fracionária = `t`
  (`fracao`); `proximoQuadro` é o seguinte, voltando ao primeiro depois do último.

Exemplo: `run` ocupa os quadros 40 a 45. Em 0,135 s de animação a 10 fps, `tempo` = 1,35.
Parte inteira 1 → quadro 40 + 1 = **41**; próximo = **42**; `t` = **0,35**. É o que o título
mostra: `quadro 41 -> 42 | t=0.35`.

## 7. Por que o VBO de posições é reenviado e o de UV não

- **UV** (`vboUvs`): a textura de cada vértice nunca muda. Montado uma vez em `Md2Model.load`,
  com `GL_STATIC_DRAW`.
- **Posições** (`vboPosicoes`): mudam a cada quadro de tela, porque a pose interpolada muda. Em
  `atualizarPose` a CPU interpola (`interpolar`), expande para a ordem dos triângulos
  (`expandir`, como o `ObjModel` faz) e reenvia com `glBufferData(..., GL_STREAM_DRAW)`.

Ou seja: o keyframe é calculado na CPU e mandado para a GPU todo quadro. Sem shaders, a GPU
não sabe interpolar sozinha. `draw` usa `glVertexPointer`, `glTexCoordPointer` e `glDrawArrays`.

## 8. Como a textura é aplicada

O MD2 guarda o UV em **pixels** (`short s, t`). Em `Md2Arquivo`, `u = s / larguraSkin` e
`v = t / alturaSkin`, ficando entre 0 e 1. O `v` **não é invertido**: o `TextureLoader` envia a
linha de cima da imagem como t = 0, que é a mesma convenção do MD2. Invertê-lo (`1 - v`) faria
cada triângulo amostrar a região espelhada na vertical do atlas da skin, e a textura sairia
embaralhada (partes erradas da imagem em cada triângulo).

Em `Main`, `Skins.listarSkinsCorpo` lista as imagens da pasta `skins/` (os nomes de skin
gravados no cabeçalho do MD2 não batem com os arquivos); `Skins.indiceInicial` tenta começar
pela skin do cabeçalho (`igdosh` no Ogro). `carregarTextura` cria uma textura por imagem, e a
tecla `T` só troca `corpo.textura`. A arma usa `weapon.jpg`.

## 9. Onde mexer para...

| Quero... | Onde |
|---|---|
| trocar o modelo | passar o caminho em `-Dexec.args=...`, ou mudar o caminho padrão em `Main.main` (variável `caminho`); `skins/` ao lado é opcional (sem ela o modelo é desenhado sem textura, com aviso no console) |
| mudar a velocidade padrão | `Animador`, campo `velocidade` (e os limites em `aumentarVelocidade` / `diminuirVelocidade`) |
| adicionar uma tecla | `Main`, no `glfwSetKeyCallback`: novo `if (tecla == GLFW_KEY_X)`; se for ação do tempo, crie o método no `Animador` |
| mudar escala, posição ou câmera | `Main`, sequência de `glTranslatef` / `glRotatef` / `glScalef` (hoje escala 0.07, recuo de 5 e giro base de −90° em Y) e o `glFrustum` |
| desligar a arma | `Main`: remover a chamada `arma.draw()` (ou apagar/renomear `weapon.md2`, aí o `Main` imprime "Sem weapon.md2") |
| mudar o loop ou a velocidade padrão | loop: `Animador.atualizar` (o `% n`) e `proximoQuadro`; velocidade padrão (10): campo `velocidade` do `Animador` |
| mudar o fundo | `Main`, `glClearColor` |

Ordem das transformações em `Main`: o código lê de cima para baixo, mas elas valem de baixo
para cima sobre o modelo: primeiro `-90°` em X (o Quake tem Z para cima), depois a escala 0.07,
o giro base de −90° em Y (o MD2 olha para +X; −90° leva +X para +Z, na direção da câmera), a rotação automática
(`angulo`, tecla `R`), a inclinação de câmera de 20° e, por fim, o recuo de 5 unidades.

Na animação `stand`, entre os quadros 20 e 35 o Ogro vira a cabeça para o lado esquerdo; isso faz parte da animação, não é erro de rotação.
