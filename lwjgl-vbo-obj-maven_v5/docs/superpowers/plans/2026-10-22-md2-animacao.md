# Animação MD2 por keyframe — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fazer o código-base `lwjgl-vbo-obj-maven_v5` abrir um MD2 (Ogro), interpolar e executar as animações por keyframe, com textura, controlado pelo teclado.

**Architecture:** Leitor de MD2 em Java puro (`Md2Arquivo`), sem OpenGL, testável com JUnit. Lógica de animação também em Java puro (`Md2Animacao`, `Animador`). A parte OpenGL fica em `Md2Model extends Model`, que interpola na CPU e reenvia o VBO de posições a cada quadro, no mesmo estilo do `ObjModel.draw()`. O `Main` é reescrito para desenhar o MD2 em vez do OBJ.

**Tech Stack:** Java 17, Maven, LWJGL 3.3.6 (GLFW + OpenGL 2.1 compatibility, sem shaders), `javax.imageio` para PNG/JPG, JUnit 5 (só escopo `test`).

**Spec:** `docs/superpowers/specs/2026-10-22-md2-animacao-design.md` (leia junto com este plano).

Todos os caminhos abaixo são relativos à raiz do projeto `lwjgl-vbo-obj-maven_v5/`, e todos os comandos rodam dali.

## Global Constraints

- Java 17 (`maven.compiler.release` 17), LWJGL 3.3.6, OpenGL 2.1 compatibility profile, **sem shaders, sem VAO**.
- Nenhuma dependência nova de runtime. JUnit só com `<scope>test</scope>`.
- Caminhos relativos à pasta de trabalho (raiz do projeto), seguindo a convenção `src/main/resources/...` do código-base.
- Estilo do código-base: Java simples, campos públicos, `ArrayList`, `FloatBuffer` via `BufferUtils`. **Nomes de classes, métodos, variáveis e comentários em português.** Sem streams encadeados, generics elaborados ou camadas extras: o aluno precisa explicar cada linha ao vivo.
- Arquivos do OBJ (`ObjModel`, `Face3D`, `GrupoFaces`, `MaterialPos`, `Object3D`, `Vector2f`, `Vector3f`, `VboCube`, `Model`, `TextureLoader`) **não são alterados**.
- Comentários em português obrigatórios em: leitura do cabeçalho e dos blocos do MD2, descompactação dos vértices, fórmula da interpolação, reenvio do VBO a cada quadro e controle do tempo.
- A pasta **não é um repositório git**. Os passos "Commit" só valem se o aluno tiver pedido `git init`; caso contrário, pule-os. Commits saem só no nome do aluno, sem `Co-Authored-By`.
- Rodar testes: `mvn -q -B test`. Rodar o programa: `mvn -q compile exec:java`.

## Review Focus

1. **`args[0]` com caminho inexistente ou arquivo que não é MD2**: deve sair com mensagem clara contendo "MD2" ou o caminho, sem travar a janela. Coberto pelos testes `rejeita*`/`arquivoInexistente*` (Task 1) e pela verificação com `-Dexec.args="pom.xml"` (Task 4).
2. **Modelo sem pasta `skins/` ou sem `weapon.md2`**: desenha sem textura e/ou sem arma, avisando no console. Coberto por `pastaInexistenteDaListaVazia` e `achaSkinDaArma` (Task 4).
3. **Skins em JPEG** (`grok.jpg`, `weapon.jpg`): o ImageIO precisa ler todas. Coberto por `skinsReaisDoOgroCarregam` (Task 4).
4. **`dt` gigante** (janela arrastada, PC engasgou): o quadro tem que continuar dentro da animação. Coberto por `dtEnormeContinuaDentroDaAnimacao` (Task 2).
5. **Windows em português** (vírgula decimal): o título tem que mostrar `t=0.35`. Coberto por `descricaoUsaPontoDecimalMesmoEmPortugues` (Task 2).

---

### Task 1: Modelo Ogro nos recursos + leitor de MD2 (`Md2Arquivo`)

**Files:**
- Modify: `pom.xml` (dependência JUnit + surefire)
- Create: `src/main/resources/md2/ogro/` (assets)
- Create: `src/main/java/aula/md2/Md2Arquivo.java`
- Test: `src/test/java/aula/md2/Md2ArquivoTest.java`

**Interfaces:**
- Produces (`aula.md2.Md2Arquivo`, todos campos `public`):
  - `Md2Arquivo(String caminho) throws IOException` e `Md2Arquivo(byte[] dados) throws IOException`
  - `int larguraSkin, alturaSkin, numVertices, numSt, numTriangulos, numQuadros`
  - `String[] skins`: nomes gravados no cabeçalho (ex.: `"players/ogro/igdosh.pcx"`)
  - `float[] uvs`: `numSt * 2`, já normalizados (`u = s / larguraSkin`, `v = t / alturaSkin`, **sem inverter v**)
  - `int[] triVertices`, `int[] triUvs`: `numTriangulos * 3` cada
  - `String[] nomesQuadros`: `numQuadros`
  - `float[][] posicoes`: `[numQuadros][numVertices * 3]`, já descompactadas (`scale * (byte & 0xFF) + translate`)

- [ ] **Step 1: Baixar os assets do Ogro**

Copiar de `https://raw.githubusercontent.com/mrdoob/three.js/dev/examples/models/md2/ogro/` para `src/main/resources/md2/ogro/`:
`ogro.md2`, `weapon.md2`, `Ogro.txt` e, em `skins/`: `arboshak.png ctf_b.png ctf_r.png darkam.png freedom.png gib.png gordogh.png grok.jpg igdosh.png khorne.png nabogro.png ogrobase.png sharokh.png weapon.jpg`.

Verificar: `ls -l src/main/resources/md2/ogro/ogro.md2` mostra 317276 bytes, `weapon.md2` 71708 bytes e `skins/` tem 14 arquivos.

- [ ] **Step 2: Adicionar JUnit ao `pom.xml`**

`org.junit.jupiter:junit-jupiter:5.10.2` com `<scope>test</scope>`, e fixar o `maven-surefire-plugin` na versão `3.2.5` no `<build><plugins>`. Manter o estilo compacto (uma linha por elemento) do `pom.xml` existente.

- [ ] **Step 3: Escrever os testes que falham**

```java
package aula.md2;
// imports: org.junit.jupiter.api.Test, static org.junit.jupiter.api.Assertions.*, java.io.IOException, java.nio.*

class Md2ArquivoTest {
    static final String OGRO = "src/main/resources/md2/ogro/ogro.md2";
    static final String ARMA = "src/main/resources/md2/ogro/weapon.md2";

    @Test void leCabecalhoDoOgro() throws IOException {
        Md2Arquivo m = new Md2Arquivo(OGRO);
        assertEquals(256, m.larguraSkin); assertEquals(256, m.alturaSkin);
        assertEquals(358, m.numVertices); assertEquals(469, m.numSt);
        assertEquals(670, m.numTriangulos); assertEquals(199, m.numQuadros);
        assertEquals(13, m.skins.length);
        assertEquals("players/ogro/igdosh.pcx", m.skins[0]);
    }

    @Test void leCabecalhoDaArma() throws IOException {
        Md2Arquivo m = new Md2Arquivo(ARMA);
        assertEquals(256, m.larguraSkin); assertEquals(128, m.alturaSkin);
        assertEquals(74, m.numVertices); assertEquals(116, m.numSt);
        assertEquals(122, m.numTriangulos); assertEquals(199, m.numQuadros);
    }

    @Test void leUvsETriangulos() throws IOException {
        Md2Arquivo m = new Md2Arquivo(OGRO);
        assertEquals(469 * 2, m.uvs.length);
        assertEquals(0.99609375f, m.uvs[0], 1e-6); // s=255 / 256
        assertEquals(0.0390625f, m.uvs[1], 1e-6);  // t=10 / 256
        assertEquals(670 * 3, m.triVertices.length);
        assertArrayEquals(new int[]{330, 332, 331}, java.util.Arrays.copyOf(m.triVertices, 3));
        assertArrayEquals(new int[]{0, 1, 2}, java.util.Arrays.copyOf(m.triUvs, 3));
    }

    @Test void descompactaPosicoesDosQuadros() throws IOException {
        Md2Arquivo m = new Md2Arquivo(OGRO);
        assertEquals(199, m.posicoes.length);
        assertEquals(358 * 3, m.posicoes[0].length);
        assertEquals("stand001", m.nomesQuadros[0]);
        assertEquals("run001", m.nomesQuadros[40]);
        assertEquals("boom", m.nomesQuadros[198]);
        assertEquals(0.562324f, m.posicoes[0][0], 1e-4);
        assertEquals(1.466234f, m.posicoes[0][1], 1e-4);
        assertEquals(-2.718453f, m.posicoes[0][2], 1e-4);
        assertEquals(-1.851437f, m.posicoes[40][0], 1e-4);
        assertEquals(-0.233660f, m.posicoes[40][1], 1e-4);
        assertEquals(0.940164f, m.posicoes[40][2], 1e-4);
        assertEquals(8.105442f, m.posicoes[0][357 * 3], 1e-4);     // último vértice
        assertEquals(-20.559797f, m.posicoes[0][357 * 3 + 2], 1e-4);
    }

    @Test void rejeitaArquivoQueNaoEMd2() {
        byte[] lixo = "# isto e um OBJ\nv 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n# mais texto para passar de 68 bytes\n".getBytes();
        IOException e = assertThrows(IOException.class, () -> new Md2Arquivo(lixo));
        assertTrue(e.getMessage().contains("MD2"), e.getMessage());
    }

    @Test void rejeitaArquivoCurtoDemais() {
        IOException e = assertThrows(IOException.class, () -> new Md2Arquivo(new byte[10]));
        assertTrue(e.getMessage().contains("MD2"), e.getMessage());
    }

    @Test void rejeitaVersaoErrada() {
        ByteBuffer b = ByteBuffer.allocate(68).order(ByteOrder.LITTLE_ENDIAN);
        b.put("IDP2".getBytes()).putInt(7);
        IOException e = assertThrows(IOException.class, () -> new Md2Arquivo(b.array()));
        assertTrue(e.getMessage().contains("MD2") && e.getMessage().contains("7"), e.getMessage());
    }

    @Test void arquivoInexistenteDaMensagemClara() {
        IOException e = assertThrows(IOException.class, () -> new Md2Arquivo("nao/existe.md2"));
        assertTrue(e.getMessage().contains("nao/existe.md2"), e.getMessage());
    }
}
```

- [ ] **Step 4: Rodar e ver falhar**

Run: `mvn -q -B test -Dtest=Md2ArquivoTest`
Expected: erro de compilação, `cannot find symbol: class Md2Arquivo`.

- [ ] **Step 5: Implementar `Md2Arquivo`**

`Md2Arquivo(String caminho)` lê com `Files.readAllBytes`. Se faltar o arquivo, lança `IOException("Arquivo não encontrado: " + caminho)` e depois delega para `Md2Arquivo(byte[])`. O construtor de bytes usa `ByteBuffer.wrap(dados).order(ByteOrder.LITTLE_ENDIAN)` e lê com posições absolutas (`getInt(ofs)`, `getShort(ofs)`, `getFloat(ofs)`, `get(ofs) & 0xFF`), seguindo o layout da seção 6.2 do spec.

Mensagens de erro:
- menos de 68 bytes ou ident diferente de `IDP2`: `"Não é um arquivo MD2"` (incluir o ident lido quando houver);
- versão diferente de 8: `"Versão de MD2 não suportada: " + versao`.

Nomes (skin de 64 bytes, quadro de 16 bytes) terminam no primeiro `\0`. Um método privado `lerTexto(ByteBuffer, int ofs, int tamanho)` evita repetição. Comentários em português explicando cada bloco do arquivo.

- [ ] **Step 6: Rodar e ver passar**

Run: `mvn -q -B test -Dtest=Md2ArquivoTest`
Expected: `Tests run: 8, Failures: 0, Errors: 0`, sem saída de erro (`-q` mostra só falhas).

- [ ] **Step 7: Commit**

```bash
git add pom.xml src/main/resources/md2 src/main/java/aula/md2/Md2Arquivo.java src/test/java/aula/md2/Md2ArquivoTest.java
git commit -m "feat: leitor de arquivos MD2 e modelo Ogro"
```

---

### Task 2: Lista de animações e controle do tempo (`Md2Animacao`, `Animador`)

**Files:**
- Create: `src/main/java/aula/md2/Md2Animacao.java`
- Create: `src/main/java/aula/md2/Animador.java`
- Test: `src/test/java/aula/md2/Md2AnimacaoTest.java`, `src/test/java/aula/md2/AnimadorTest.java`

**Interfaces:**
- Consumes: `Md2Arquivo.nomesQuadros` (Task 1).
- Produces (`aula.md2.Md2Animacao`): `public String nome; public int inicio; public int fim;`, `Md2Animacao(String nome, int inicio, int fim)`, `int numQuadros()` (`fim - inicio + 1`), `static ArrayList<Md2Animacao> montarLista(String[] nomesQuadros)`.
- Produces (`aula.md2.Animador`):
  - `Animador(ArrayList<Md2Animacao> animacoes)`
  - campos `public int velocidade = 10; public boolean interpolar = true; public boolean pausado = false;`
  - `void atualizar(double dt)`, `void proximaAnimacao()`, `void animacaoAnterior()`, `void aumentarVelocidade()`, `void diminuirVelocidade()`, `void alternarInterpolacao()`, `void alternarPausa()`
  - `Md2Animacao animacaoAtual()`, `int indiceAnimacao()` (base 0), `int quadroAtual()`, `int proximoQuadro()`, `float fracao()`, `String descricao()`

- [ ] **Step 1: Escrever os testes que falham**

`Md2AnimacaoTest`:
- `agrupaAs21AnimacoesDoOgro`: `montarLista(new Md2Arquivo(OGRO).nomesQuadros)` tem tamanho 21. Conferir `(nome, inicio, fim)` dos índices 0 `stand 0 39`, 1 `run 40 45`, 3 `paina 54 57`, 8 `salute_alt 84 94`, 19 `deathc 190 197` e 20 `boom 198 198`.
- `agrupaSoQuadrosConsecutivos`: `montarLista(new String[]{"a1","a2","b1","a3"})` resulta em `a 0 1`, `b 2 2`, `a 3 3`.

`AnimadorTest` usa uma lista fixa, sem arquivo: `stand 0–39`, `run 40–45`, `boom 198–198` (3 animações).

```java
@Test void comecaNoPrimeiroQuadroDaPrimeiraAnimacao() // quadroAtual 0, proximoQuadro 1, fracao 0, velocidade 10, interpolar true, pausado false
@Test void avancaEInterpola()                    // proximaAnimacao(); atualizar(0.135) -> quadroAtual 41, proximoQuadro 42, fracao 0.35 (±1e-4)
@Test void ultimoQuadroVoltaParaOPrimeiro()      // run; atualizar(0.55) -> quadroAtual 45, proximoQuadro 40
@Test void dtEnormeContinuaDentroDaAnimacao()    // run; atualizar(1000.37) -> 40<=quadroAtual<=45, 40<=proximoQuadro<=45, 0<=fracao<1
@Test void animacaoDeUmQuadroFicaParada()        // animacaoAnterior() (vai para boom); atualizar(0.37) -> quadroAtual 198, proximoQuadro 198
@Test void setasDaoAVolta()                      // animacaoAnterior() a partir de 0 -> indiceAnimacao 2; proximaAnimacao() -> 0
@Test void trocarAnimacaoRecomecaDoPrimeiroQuadro() // atualizar(0.5); proximaAnimacao() -> quadroAtual 40, fracao 0
@Test void interpolacaoDesligadaDaFracaoZero()   // run; atualizar(0.135); alternarInterpolacao() -> fracao 0, quadroAtual 41, interpolar false
@Test void pausadoNaoAvanca()                    // alternarPausa(); atualizar(1.0) -> quadroAtual 0, pausado true
@Test void velocidadeVaiDe1a60()                 // 10 -aumentar-> 15; -diminuir-> 10; -diminuir-> 9; 20x diminuir -> 1; 30x aumentar -> 60
@Test void descricaoUsaPontoDecimalMesmoEmPortugues()
    // Locale.setDefault(Locale.forLanguageTag("pt-BR")) (restaurar o anterior num finally);
    // proximaAnimacao(); atualizar(0.135);
    // assertEquals("run (2/3) | quadro 41 -> 42 | t=0.35 | interp ON | 10 fps", a.descricao());
    // alternarInterpolacao(); alternarPausa(); descricao() contém "interp OFF" e termina com " | PAUSADO"
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `mvn -q -B test -Dtest=Md2AnimacaoTest+AnimadorTest`
Expected: erro de compilação, `cannot find symbol: class Md2Animacao`.

- [ ] **Step 3: Implementar `Md2Animacao.montarLista`**

O prefixo é o nome sem os dígitos finais (`nome.replaceAll("\\d+$", "")`). Percorrer os quadros em ordem: se o prefixo é igual ao da última animação da lista, estende o `fim` dela; senão, cria uma nova.

- [ ] **Step 4: Implementar `Animador`**

Guardar `double tempo`, a posição dentro da animação atual medida em keyframes, em `[0, n)`, onde `n = animacaoAtual().numQuadros()`:

```
atualizar(dt):  se !pausado: tempo = (tempo + dt * velocidade) % n     // % trata dt enorme
quadroAtual():  inicio + (int) floor(tempo)
proximoQuadro(): inicio + ((int) floor(tempo) + 1) % n                  // último volta pro primeiro
fracao():       interpolar ? (float) (tempo - floor(tempo)) : 0
```

- Trocar de animação zera `tempo`; as setas dão a volta na lista.
- Velocidade: `aumentar` faz `v < 10 ? v + 1 : v + 5` com máximo 60; `diminuir` faz `v > 10 ? v - 5 : v - 1` com mínimo 1.
- `descricao()` usa `String.format(Locale.US, "%s (%d/%d) | quadro %d -> %d | t=%.2f | interp %s | %d fps", ...)`, com `fracao()` no `t`, mais `" | PAUSADO"` se pausado.
- Comentários em português explicando o tempo em keyframes e o `%`.

- [ ] **Step 5: Rodar e ver passar**

Run: `mvn -q -B test`
Expected: todos os testes passam (8 da Task 1 + 13 desta).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/aula/md2/Md2Animacao.java src/main/java/aula/md2/Animador.java src/test/java/aula/md2/Md2AnimacaoTest.java src/test/java/aula/md2/AnimadorTest.java
git commit -m "feat: lista de animacoes e controle de tempo dos keyframes"
```

---

### Task 3: Interpolação e VBO (`Md2Model`)

**Files:**
- Create: `src/main/java/aula/md2/Md2Model.java`
- Test: `src/test/java/aula/md2/Md2ModelTest.java`

**Interfaces:**
- Consumes: `Md2Arquivo` (Task 1); `aula.Model.Model` (abstrata, `load()`/`draw()`).
- Produces (`aula.md2.Md2Model extends Model`):
  - `public Md2Arquivo arquivo; public int textura = 0;` (0 = sem textura)
  - `Md2Model(String caminho) throws IOException`: lê o arquivo e chama `load()`. **Precisa de contexto OpenGL ativo.**
  - `void load()`: cria o VBO de UV (estático) e o VBO de posições
  - `void atualizarPose(int quadroA, int quadroB, float t)`: interpola e reenvia o VBO de posições
  - `void draw()`: liga `textura` e desenha `numTriangulos * 3` vértices
  - `static void interpolar(float[] a, float[] b, float t, float[] saida)`
  - `static void expandir(float[] posVertices, int[] triVertices, FloatBuffer saida)`: `clear()`, coloca os 3 floats de cada índice em ordem e faz `flip()`

- [ ] **Step 1: Escrever os testes que falham** (só os métodos estáticos; não precisam de OpenGL)

```java
@Test void interpolaUmQuartoDoCaminho() {
    float[] s = new float[3];
    Md2Model.interpolar(new float[]{0, 10, -4}, new float[]{10, 20, 4}, 0.25f, s);
    assertArrayEquals(new float[]{2.5f, 12.5f, -2f}, s, 1e-6f);
}
@Test void tZeroDaAETUmDaB()   // t=0 -> igual a A; t=1 -> igual a B
@Test void expandePelosTriangulos() {
    FloatBuffer out = FloatBuffer.allocate(9);
    Md2Model.expandir(new float[]{0,0,0, 1,1,1, 2,2,2}, new int[]{2, 0, 1}, out);
    assertEquals(0, out.position()); assertEquals(9, out.limit());
    float[] r = new float[9]; out.get(r);
    assertArrayEquals(new float[]{2,2,2, 0,0,0, 1,1,1}, r);
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `mvn -q -B test -Dtest=Md2ModelTest`
Expected: erro de compilação, `cannot find symbol: class Md2Model`.

- [ ] **Step 3: Implementar `Md2Model`**

- `interpolar`: `saida[i] = a[i] + (b[i] - a[i]) * t`, com comentário mostrando a fórmula.
- `load()`: monta um `FloatBuffer` de `numTriangulos * 3 * 2` usando `triUvs` e `uvs`, e envia com `glBufferData(..., GL_STATIC_DRAW)`. Depois gera o VBO de posições, aloca os buffers de trabalho (`float[numVertices*3]` e `BufferUtils.createFloatBuffer(numTriangulos*3*3)`) e chama `atualizarPose(0, 0, 0)`.
- `atualizarPose`: `interpolar(posicoes[a], posicoes[b], t, ...)`, depois `expandir(...)`, depois `glBindBuffer` + `glBufferData(GL_ARRAY_BUFFER, buffer, GL_STREAM_DRAW)`. O comentário deve deixar claro que "a pose é calculada na CPU e reenviada para a GPU todo quadro".
- `draw()`: `glBindTexture(GL_TEXTURE_2D, textura)` e o mesmo bloco de `glEnableClientState`/`glVertexPointer`/`glTexCoordPointer`/`glDrawArrays`/disable do `ObjModel.draw()` (linhas 720–733), com contagem `numTriangulos * 3`.

- [ ] **Step 4: Rodar e ver passar**

Run: `mvn -q -B test`
Expected: todos os testes passam (24 no total). Rodar também `mvn -q -B compile`, que deve terminar sem erros.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/aula/md2/Md2Model.java src/test/java/aula/md2/Md2ModelTest.java
git commit -m "feat: interpolacao de keyframes e VBO dinamico do MD2"
```

---

### Task 4: Skins, `Main` novo e teclado

**Files:**
- Create: `src/main/java/aula/md2/Skins.java`
- Modify: `src/main/java/aula/Main.java` (reescrever o conteúdo do `main`)
- Test: `src/test/java/aula/md2/SkinsTest.java`

**Interfaces:**
- Consumes: `Md2Model`, `Animador`, `Md2Animacao.montarLista`, `TextureLoader.loadTexture(BufferedImage)`.
- Produces (`aula.md2.Skins`, métodos estáticos):
  - `ArrayList<File> listarSkinsCorpo(File pasta)`: `.png/.jpg/.jpeg` (sem diferenciar maiúsculas), sem os arquivos cujo nome começa com `weapon`, em ordem alfabética; pasta inexistente resulta em lista vazia
  - `File acharSkinArma(File pasta)`: primeiro `weapon.png/.jpg/.jpeg`, ou `null`
  - `int indiceInicial(ArrayList<File> skins, String[] skinsCabecalho)`: posição do arquivo cujo nome sem extensão é igual ao nome base do `skinsCabecalho[0]` (`"players/ogro/igdosh.pcx"` → `igdosh`); se não achar, ou se o cabeçalho estiver vazio, `0`
  - `String nomeSemExtensao(File f)`

- [ ] **Step 1: Escrever os testes que falham**

```java
@Test void listaSkinsDoCorpoEmOrdemSemAArma(@TempDir Path d)
    // criar b.png, a.jpg, C.JPEG, weapon.jpg, notas.txt -> nomes == [a.jpg, b.png, C.JPEG]
@Test void pastaInexistenteDaListaVazia()          // listarSkinsCorpo(new File("nao/existe")).isEmpty()
@Test void achaSkinDaArma(@TempDir Path d)         // sem arquivo -> null; com weapon.jpg -> nome "weapon.jpg"
@Test void indiceInicialUsaPrimeiroSkinDoCabecalho()
    // skins [arboshak.png, igdosh.png]: cabeçalho {"players/ogro/igdosh.pcx"} -> 1; {"x/naoexiste.pcx"} -> 0; {} -> 0
@Test void skinsReaisDoOgroCarregam() throws IOException
    // lista = listarSkinsCorpo(new File("src/main/resources/md2/ogro/skins")) -> 13 arquivos;
    // lista.get(indiceInicial(lista, new Md2Arquivo(OGRO).skins)).getName() == "igdosh.png";
    // para cada arquivo: ImageIO.read(f) != null e largura 256;
    // ImageIO.read(acharSkinArma(...)) tem 256 x 128
```

Ordenação: decidida como **alfabética sem diferenciar maiúsculas pelo nome** (`String.CASE_INSENSITIVE_ORDER`). O primeiro teste espera `[a.jpg, b.png, C.JPEG]`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `mvn -q -B test -Dtest=SkinsTest`
Expected: erro de compilação, `cannot find symbol: class Skins`.

- [ ] **Step 3: Implementar `Skins`**

`File.listFiles()` (retorna `null` se a pasta não existe, o que vira lista vazia), filtro por extensão com `toLowerCase()`, e `sort` pelo nome com `String.CASE_INSENSITIVE_ORDER`.

- [ ] **Step 4: Rodar e ver passar**

Run: `mvn -q -B test`
Expected: todos os testes passam (29 no total).

- [ ] **Step 5: Reescrever `Main`**

Manter a criação da janela, `glfwSwapInterval(1)`, o fundo cinza, o `GL_DEPTH_TEST` e o `try/finally` que destrói a janela. Título inicial: `"MD2"`. Mudanças:

- Caminho padrão: `src/main/resources/md2/ogro/ogro.md2`, com `args[0]` substituindo. A pasta do modelo é a pasta do arquivo.
- Carga dentro do `try`: `new Md2Model(caminho)`. Em caso de `IOException`: `System.err.println("Erro ao abrir MD2: " + e.getMessage())` e sair pelo `finally`, sem stack trace.
- Arma: se existir `weapon.md2` na pasta, criar outro `Md2Model`; senão, `null` e uma linha no console.
- Skins: `Skins.listarSkinsCorpo(new File(pasta, "skins"))`. Carregar **todas** as texturas no início (`TextureLoader.loadTexture(ImageIO.read(f))`) num `int[]`, começando em `indiceInicial(...)`. Lista vazia: aviso no console e `textura = 0`. A arma usa `acharSkinArma`.
- `Animador` criado com `Md2Animacao.montarLista(corpo.arquivo.nomesQuadros)`.
- Teclado com `glfwSetKeyCallback`, agindo só em `action == GLFW_PRESS`:
  - `RIGHT`/`LEFT`: próxima/anterior animação;
  - `I`: interpolação;
  - `EQUAL` e `KP_ADD`: aumentar velocidade; `MINUS` e `KP_SUBTRACT`: diminuir;
  - `SPACE`: pausa;
  - `T`: próxima skin (dá a volta);
  - `R`: rotação;
  - `ESCAPE`: fecha.

  O `W` continua por `glfwGetKey` (segurar), como no código-base. A tecla `S` sai.
- Loop: `dt` pela diferença de `glfwGetTime()`; `animador.atualizar(dt)`; ângulo `+= dt * 25` se a rotação estiver ligada; `corpo.atualizarPose(animador.quadroAtual(), animador.proximoQuadro(), animador.fracao())`, e o mesmo para a arma.
- Desenho: `glEnable(GL_TEXTURE_2D)`, `glColor3f(1,1,1)`, depois corpo e arma.
- Transformações (os valores finais o executor ajusta pela captura de tela):
  `glTranslatef(0, 0, -5)` → `glRotatef(20, 1, 0, 0)` (inclinação fixa) → `glRotatef(angulo, 0, 1, 0)` → `glRotatef(-90, 0, 1, 0)` (olhar para a câmera) → `glScalef(0.04f, …)` → `glRotatef(-90, 1, 0, 0)` (Z-para-cima do Quake vira Y-para-cima).
  Requisito: o Ogro inteiro visível, em pé e, com a rotação parada no ângulo 0, de frente para a câmera.
- Título a cada 0,1 s: `"MD2 | " + animador.descricao() + " | skin " + Skins.nomeSemExtensao(skinAtual)` (`"sem textura"` se não houver skin).

- [ ] **Step 6: Verificar rodando o programa**

1. `mvn -q -B compile`: sem erros.
2. Rodar `mvn -q exec:java` em segundo plano. Quando a janela abrir, ler o título com PowerShell: `(Get-Process java | Where-Object { $_.MainWindowTitle -like 'MD2*' }).MainWindowTitle`. Esperado: começa com `MD2 | stand (1/21) | quadro ` e termina com `| skin igdosh`.
3. Capturar a tela com `System.Drawing` (`Graphics.CopyFromScreen` do `PrimaryScreen.Bounds`) para um PNG no scratchpad e abrir a imagem. Esperado: Ogro texturizado, inteiro, em pé.
4. Mandar teclas com `$w = New-Object -ComObject WScript.Shell; $w.AppActivate('MD2'); $w.SendKeys(...)` e reler o título a cada tecla:
   - `{RIGHT}` → contém `run (2/21)`
   - `i` → `interp OFF` (e `i` de novo → `interp ON`)
   - `=` → `15 fps`
   - `{SUBTRACT}` → `10 fps`
   - ` ` (espaço) → termina com `| PAUSADO` (espaço de novo despausa)
   - `t` → `skin khorne`
   - `{LEFT}{LEFT}` → `boom (21/21)`
   - `r` + captura de tela → Ogro parado, de frente
5. `{ESC}` fecha a janela e o processo `java` termina.
6. `mvn -q exec:java "-Dexec.args=pom.xml"` imprime `Erro ao abrir MD2: Não é um arquivo MD2...` e termina sozinho.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/aula/md2/Skins.java src/main/java/aula/Main.java src/test/java/aula/md2/SkinsTest.java
git commit -m "feat: Main desenha MD2 animado com skins e controles de teclado"
```

---

### Task 5: Documentação (README + EXPLICACAO.md)

**Files:**
- Modify: `README.md` (reescrever)
- Create: `EXPLICACAO.md`

**Interfaces:**
- Consumes: nomes reais de classes, métodos e teclas das Tasks 1–4.

- [ ] **Step 1: Reescrever `README.md`**

Seções: o que o programa faz (MD2 animado com interpolação e textura, sobre o código-base v5); requisitos (iguais aos atuais); como rodar (`mvn compile exec:java`, outro MD2 com `-Dexec.args=...`, que espera a pasta `skins/` e o `weapon.md2` opcional ao lado); tabela de controles (seção 6.7 do spec, sem a tecla `S`); estrutura do pacote `aula.md2`; créditos do Ogro (Magarnigal, skins de Ogro_Fix e outros, ver `src/main/resources/md2/ogro/Ogro.txt`; obtido do repositório do three.js); nota de que as classes do OBJ continuam no projeto mas não são usadas pelo `Main`. Remover as frases do README antigo que ficaram falsas.

- [ ] **Step 2: Escrever `EXPLICACAO.md`**

Português, didático, seguindo a seção 6.8 do spec:
1. o que é animação por keyframe;
2. formato MD2, com um diagrama em texto dos blocos do arquivo (cabeçalho → skins → st → triângulos → quadros) e os campos do cabeçalho;
3. descompactação `scale × byte + translate`, com o vértice 0 do quadro 0 do Ogro como exemplo (bytes `95, 129, 105` → `0.562, 1.466, -2.718`);
4. como as 21 animações são montadas a partir dos nomes;
5. a fórmula `A + (B − A) × t`, com exemplo numérico, e o que muda com a interpolação desligada;
6. tempo em keyframes (`tempo % n`, 10 fps do Quake 2, por que `run` em `t=0.135 s` está entre os quadros 41 e 42 com `t=0.35`);
7. por que o VBO de posições é reenviado a cada quadro e o de UV não;
8. como a textura é aplicada (UV em pixels ÷ tamanho da skin; por que não inverte o v);
9. tabela "onde mexer para…": trocar o modelo, mudar a velocidade padrão, adicionar uma tecla, mudar escala/câmera, desligar a arma, apontando arquivo e método.

- [ ] **Step 3: Verificar a documentação**

Para cada classe, método e tecla citados nos dois arquivos, confirmar que existem (`grep -rn "<nome>" src/main/java`). O exemplo numérico da seção 3 deve bater com o teste `descompactaPosicoesDosQuadros`. Os comandos do README já foram executados na Task 4.

- [ ] **Step 4: Commit**

```bash
git add README.md EXPLICACAO.md
git commit -m "docs: README e explicacao didatica da animacao MD2"
```
