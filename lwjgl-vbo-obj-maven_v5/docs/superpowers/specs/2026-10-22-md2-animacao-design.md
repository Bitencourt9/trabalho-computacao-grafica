# Design: animação por keyframe com MD2 no código-base v5

Data: 2026-10-22
Projeto: `lwjgl-vbo-obj-maven_v5` (código-base do professor, repositório `DennisUnivali/ComputacaoGrafica2026`)

## 1. Intenção e porquê

Trabalho da disciplina de Computação Gráfica. O enunciado pede, usando o código-base
`lwjgl-vbo-obj-maven_v5`:

- carregar arquivos de animação por keyframe do tipo **MD2** (Quake 2);
- **interpolar** as animações;
- **texturizar**;
- **executar** as animações.

O professor permite usar uma biblioteca para abrir o MD2, mas a renderização tem que ser
feita no código. O "QFrame" do enunciado é entendido como **keyframe**.

**O que define sucesso:** na apresentação, o professor vai pedir para rodar o código na frente
dele, mexer e perguntar "como você faz tal coisa". O aluno precisa conseguir **explicar e alterar
cada parte ao vivo**. Por isso, clareza e semelhança com o estilo do código-base valem mais que
desempenho ou elegância.

## 2. Objetivos

1. Ler um arquivo `.md2` com um leitor **escrito à mão** (sem biblioteca), no mesmo estilo do
   `ObjModel`: cabeçalho, coordenadas de textura, triângulos e quadros (keyframes).
2. Montar a lista de animações automaticamente a partir dos nomes dos quadros.
3. Interpolar linearmente entre o keyframe atual e o próximo, a cada quadro de tela:
   `posição = A + (B − A) × t`.
4. Renderizar com **VBO**, como no código-base: VBO de UV estático e VBO de posições
   reenviado a cada quadro com os vértices interpolados.
5. Texturizar com as skins do modelo, usando o `TextureLoader` existente, com troca de skin ao vivo.
6. Executar as animações com controle pelo teclado.
7. Mostrar o estado atual na barra de título da janela.
8. Desenhar a arma do personagem (`weapon.md2`) sincronizada com o corpo.
9. Documentar: README atualizado e um `EXPLICACAO.md` didático em português.

## 3. Não-objetivos

- Iluminação e normais. O MD2 guarda um índice para a tabela de 162 normais do Quake 2; não é
  preciso carregá-la, porque o código-base não liga `GL_LIGHTING`.
- Shaders, VAO ou GLSL. Continua o pipeline de compatibilidade do OpenGL 2.1.
- Ler PCX. As skins usadas são PNG/JPG.
- Usar os `glcmds` (triangle strips/fans) do MD2. Usar a lista de triângulos.
- Transição suave entre duas animações diferentes. Ao trocar de animação, começa do primeiro
  quadro da nova.
- MD3 ou outros formatos.
- Mostrar o avião OBJ. A tela mostra **só o personagem MD2** (decisão do aluno).
- Corrigir os bugs conhecidos do `ObjModel`. Ele não é usado pela nova tela.

## 4. Anti-objetivos (falhou mesmo que funcione se...)

- O aluno não conseguir explicar alguma parte: uso de biblioteca que esconde a leitura do
  arquivo, código "esperto" demais (streams encadeados, generics, padrões de projeto
  desnecessários, muitas camadas).
- Exigir shaders ou mudar o perfil OpenGL do código-base.
- Descaracterizar o código-base: apagar ou reescrever os arquivos do OBJ, mudar a estrutura de
  pacotes existente, trocar o Maven.
- Comentários só em inglês, ou ausência de comentários nas partes que o professor vai perguntar.

## 5. Restrições

- Java 17 (`maven.compiler.release` do `pom.xml`), LWJGL 3.3.6, Maven, `mvn compile exec:java`
  executado a partir da raiz do projeto. O alvo principal é Windows x64; os perfis de natives do
  `pom.xml` continuam valendo.
- OpenGL 2.1 compatibility profile (`glfwWindowHint` 2.1, como no `Main` atual).
- **Nenhuma dependência nova de runtime.** PNG/JPG são lidos com `javax.imageio.ImageIO`, que já
  é usado no projeto.
- Os caminhos são relativos à pasta de trabalho, seguindo a convenção do código-base
  (`src/main/resources/...`).
- Estilo do código igual ao do código-base: Java simples, campos públicos ou de pacote,
  `ArrayList`, `FloatBuffer` com `BufferUtils`, nomes e comentários em português.

## 6. Decisões já tomadas

### 6.1 Modelo de teste: Ogro

O repositório do professor não tem nenhum MD2. Será usado o **Ogro** (autor Magarnigal,
skins de Ogro_Fix e outros; liberado para uso), copiado do repositório do three.js:
`https://github.com/mrdoob/three.js/tree/dev/examples/models/md2/ogro`.

Arquivos a copiar para `src/main/resources/md2/ogro/`:

- `ogro.md2`, `weapon.md2`
- `skins/*.png` e `skins/*.jpg` (13 skins do corpo + `weapon.jpg`)
- `Ogro.txt` (créditos/licença do autor; manter junto e citar no README)

Dados verificados no arquivo (servem de referência para testes):

| | `ogro.md2` | `weapon.md2` |
|---|---|---|
| ident / versão | `IDP2` / 8 | `IDP2` / 8 |
| skin (largura × altura) | 256 × 256 | 256 × 128 |
| vértices por quadro | 358 | 74 |
| coordenadas de textura (st) | 469 | 116 |
| triângulos | 670 | 122 |
| quadros | 199 | 199 |

Os nomes de skin gravados dentro do MD2 (`players/ogro/igdosh.pcx`, etc.) **não batem** com os
arquivos disponíveis (há PCX citados que não existem em PNG, e PNGs que não estão no cabeçalho).
Por isso, **as skins são descobertas listando a pasta `skins/`**, e não pelos nomes do cabeçalho.
O `weapon.jpg` usa a skin da arma, e a skin inicial do corpo é `igdosh.png`.

### 6.2 Formato MD2 (o que o leitor precisa ler)

Tudo em little-endian. Ler o arquivo inteiro (`Files.readAllBytes`) e envolver num `ByteBuffer`
com `ByteOrder.LITTLE_ENDIAN`.

- **Cabeçalho**, 17 `int`s (68 bytes): `ident` ("IDP2"), `version` (8), `skinwidth`,
  `skinheight`, `framesize`, `num_skins`, `num_vertices`, `num_st`, `num_tris`, `num_glcmds`,
  `num_frames`, `ofs_skins`, `ofs_st`, `ofs_tris`, `ofs_frames`, `ofs_glcmds`, `ofs_end`.
  Validar `ident` e `version`, com mensagem clara se o arquivo não for MD2.
- **Coordenadas de textura** (`ofs_st`): `num_st` × (`short s`, `short t`), em pixels.
  `u = s / skinwidth`, `v = t / skinheight`. **Não inverter o v.** O `TextureLoader` envia a
  linha 0 da imagem (topo) como t = 0, que é a mesma convenção do MD2.
- **Triângulos** (`ofs_tris`): `num_tris` × (`short vertice[3]`, `short st[3]`). Os índices de
  vértice e de UV são **separados**, como no OBJ.
- **Quadros** (`ofs_frames`, cada um com `framesize` bytes): `float scale[3]`,
  `float translate[3]`, `char nome[16]` (terminado em `\0`), e depois `num_vertices` ×
  (`unsigned byte x, y, z`, `unsigned byte indiceNormal`). Posição real:
  `pos = scale × byte + translate` (o byte é lido sem sinal: `& 0xFF`). O índice da normal é ignorado.

Na carga, descompactar todos os quadros para `float[numQuadros][numVertices * 3]`.

### 6.3 Lista de animações

Percorrer os nomes dos quadros, tirar os dígitos do final (`stand001` → `stand`) e agrupar
quadros **consecutivos** com o mesmo prefixo. No Ogro isso dá 21 animações, que é o resultado
esperado:

`stand (0–39), run (40–45), attack (46–53), paina (54–57), painb (58–61), painc (62–65),
jump (66–71), flip (72–83), salute_alt (84–94), bumflop (95–111), wavealt (112–122),
sniffsniff (123–134), cstand (135–153), cwalk (154–159), crattack (160–168), crpain (169–172),
crdeath (173–177), deatha (178–183), deathb (184–189), deathc (190–197), boom (198)`

A arma tem nomes ligeiramente diferentes (`stand1`, `salutewep`, ...), mas **os mesmos índices**.
A lista vem do corpo, e a arma é desenhada com os mesmos índices de quadro.

### 6.4 Interpolação e tempo

- A velocidade é medida em keyframes por segundo. O padrão é **10** (o do Quake 2).
- A cada quadro de tela, o tempo acumulado da animação avança `dt × velocidade`, e não avança
  se estiver pausado. Daí saem o índice `atual`, o índice `próximo` e a fração `t` (0 a 1).
- `próximo` é `atual + 1` dentro da mesma animação. **No último quadro, volta para o primeiro**
  (todas as animações fazem loop, inclusive as de morte; é mais simples de demonstrar).
  Uma animação de um único quadro (`boom`) fica parada.
- Com a **interpolação desligada**, desenhar só o quadro `atual` (`t` tratado como 0).
  É assim que o professor vê a diferença.
- Interpolar por vértice único (358), e depois expandir pelos triângulos para o VBO.

### 6.5 Renderização (mesmo espírito do `ObjModel.draw()`)

- Expandir os triângulos em vértices soltos (`num_tris × 3`), como o `ObjModel` faz.
- **VBO de UV**: montado uma vez na carga (`GL_STATIC_DRAW`).
- **VBO de posições**: criado na carga e **reenviado a cada quadro** com as posições
  interpoladas (`glBufferData` ou `glBufferSubData`, com `GL_DYNAMIC_DRAW` ou `GL_STREAM_DRAW`).
  Este é o ponto-chave a explicar: "o keyframe é calculado na CPU e mandado para a GPU todo quadro".
- Desenhar com `glEnableClientState` + `glVertexPointer` / `glTexCoordPointer` + `glDrawArrays`,
  exatamente como o `ObjModel.draw()`.
- O MD2 é Z-para-cima: rotacionar −90° em X para o personagem ficar em pé. Escala e posição
  devem ser ajustadas para o Ogro caber na tela (ele mede cerca de 27 × 37 × 52 unidades).
- O corpo e a arma usam a mesma classe, cada um com sua textura.

### 6.6 Estrutura do código

- Novo pacote `aula.md2`, com a classe principal do modelo (`Md2Model extends Model`, seguindo
  o padrão do `ObjModel`: o construtor recebe o caminho, lê o arquivo e chama `load()` para criar
  os VBOs; `draw()` desenha o estado atual).
- O estado da animação (animação atual, tempo, velocidade, pausa, interpolação ligada) pode ficar
  numa classe pequena separada ou no próprio modelo; o importante é ser fácil de mostrar.
- `Main` passa a abrir o MD2 em vez do OBJ. Caminho padrão:
  `src/main/resources/md2/ogro/ogro.md2`. Aceitar outro caminho via `args[0]`, como hoje. A arma é
  carregada se existir `weapon.md2` na mesma pasta.
- Arquivos do OBJ (`ObjModel`, `Face3D`, `VboCube`, etc.) **permanecem intocados**.
- Comentários em português nas partes de leitura do arquivo, fórmula da interpolação, envio ao
  VBO e controle do tempo.

### 6.7 Tela e controles

Na tela: só o Ogro com a skin e a arma, fundo cinza como no código-base, rotação automática lenta
em Y.

A barra de título mostra o estado, por exemplo:
`MD2 | run (2/21) | quadro 41 -> 42 | t=0.35 | interp ON | 10 fps | skin igdosh`.

| Tecla | Ação |
|---|---|
| `←` / `→` | animação anterior / próxima |
| `I` | liga/desliga interpolação |
| `+` / `-` | aumenta/diminui a velocidade |
| `Espaço` | pausa/continua |
| `T` | próxima skin |
| `W` (segurar) | wireframe (comportamento já existente) |
| `R` | liga/desliga rotação automática |
| `ESC` | fecha |

As teclas de alternância (`I`, `Espaço`, `T`, `R`, setas, `+`/`-`) devem disparar **uma vez por
toque**, não a cada quadro enquanto seguradas. O `W` continua "segurar", como no código-base.
O teclado do aluno é ABNT2: `+`/`-` devem funcionar tanto na tecla `=`/`-` da fileira principal
quanto no teclado numérico.

### 6.8 Documentação

- **README.md**: atualizar para o novo comportamento (como rodar, controles, de onde vem o
  modelo e os créditos do Ogro) e remover o que ficou falso.
- **EXPLICACAO.md** (português, didático): o que é animação por keyframe; o formato MD2 com o
  desenho dos blocos do arquivo; como os vértices são descompactados; como as animações são
  agrupadas; a fórmula da interpolação com exemplo numérico; por que o VBO de posições é
  reenviado a cada quadro; como a textura é aplicada; mapa de "onde mexer para..." (trocar o
  modelo, mudar a velocidade padrão, adicionar uma tecla, etc.), pensado para a apresentação.

## 7. Deixado para quem for construir

- Nomes exatos das classes auxiliares e se o estado da animação fica em classe separada.
- `glBufferData` vs `glBufferSubData` e `GL_DYNAMIC_DRAW` vs `GL_STREAM_DRAW`.
- Escala, posição e inclinação da câmera para enquadrar bem o personagem.
- Limites e passo da velocidade (sugestão: 1 a 60, passo 1 abaixo de 10 e passo 5 acima, para
  conseguir câmera lenta e mostrar a interpolação).
- Frequência de atualização do título da janela.
- Testes automatizados para o leitor (por exemplo, conferir os números da tabela 6.1 e as 21
  animações da 6.3). Se usar JUnit, ele entra só com escopo `test`.

## 8. Decisões tomadas pelo assistente (não pedidas explicitamente pelo aluno)

- Leitor de MD2 à mão em vez de biblioteca (motivo: o aluno precisa explicar a leitura).
- Ogro como modelo e skins descobertas pela pasta.
- Arma desenhada junto.
- Todas as animações em loop; troca de animação sem transição.
- Estado na barra de título (não há renderização de texto no OpenGL antigo).
- Mapa de teclas da seção 6.7.
- Arquivos do OBJ mantidos sem alteração.
- `EXPLICACAO.md` para estudo.
- Setas dão a volta na lista (depois da última animação vem a primeira, e vice-versa).
- Se o modelo passado em `args[0]` não tiver pasta `skins/` com PNG/JPG, desenha sem textura
  (branco) e avisa no console, sem travar.
- A tecla `S` do código-base (tirar a inclinação) sai; a inclinação de 20° fica fixa.
- Números no título formatados com `Locale.US` (`t=0.35`, não `t=0,35`).
