# Aula: modelo MD2 animado com LWJGL 3 e Maven

Programa em Java que carrega um modelo **MD2** (formato de personagens do Quake 2), anima os
quadros-chave com **interpolação** feita na CPU e desenha com **VBO** e **textura**, sobre o
código-base v5 (OpenGL 2.1 compatibility profile, sem shaders e sem VAO). O modelo de teste é o
Ogro, com 21 animações e 13 skins. Para entender o código por dentro, leia o [EXPLICACAO.md](EXPLICACAO.md).

Requisitos: JDK 17+, Maven 3.9+ e driver com suporte a OpenGL 2.1 (compatibility profile).

## Executar

Na raiz do projeto:

```bash
mvn compile exec:java
```

Para carregar outro arquivo MD2:

```bash
mvn compile exec:java -Dexec.args="caminho/para/modelo.md2"
```

O programa espera, na mesma pasta do `.md2`, uma pasta `skins/` com as imagens (`.png`, `.jpg` ou
`.jpeg`) e, opcionalmente, um `weapon.md2` (a arma, desenhada junto) com `skins/weapon.png` ou
`skins/weapon.jpg`. Sem `skins/` o modelo aparece sem textura (com aviso no console); sem
`weapon.md2` aparece só o corpo. Imagens inválidas geram um aviso no console, mas a posição continua na lista com textura 0: apertar `T` nela mostra o modelo branco (sem textura) com o nome dessa skin.

No Windows, também é possível abrir o projeto como **Maven Project** no IntelliJ ou Eclipse e executar `aula.Main`.

No macOS, a janela GLFW pode exigir execução com `-XstartOnFirstThread`. Se necessário, rode pela IDE com essa opção na JVM (o `exec:java` não cria outra JVM para aplicar esse argumento).

Testes: `mvn -q -B test`.

## Controles

A barra de título mostra o estado, por exemplo:
`MD2 | run (2/21) | quadro 41 -> 42 | t=0.35 | interp ON | 10 fps | skin igdosh`.

| Tecla | Ação |
|---|---|
| `←` / `→` | animação anterior / próxima |
| `I` | liga/desliga a interpolação |
| `+` / `-` | aumenta/diminui a velocidade (1 a 60 keyframes por segundo; padrão 10) |
| `Espaço` | pausa/continua |
| `T` | próxima skin |
| `R` | liga/desliga a rotação automática |
| `W` (segurar) | wireframe |
| `ESC` | fecha a janela |

Só o `W` age enquanto está segurado; as outras teclas agem uma vez por toque. `+` e `-`
funcionam na fileira principal (teclas `=` e `-`) e no teclado numérico.

## Estrutura do pacote `aula.md2`

| Classe | Papel |
|---|---|
| `Md2Arquivo` | lê o arquivo `.md2` (cabeçalho, UVs, triângulos, quadros) e descompacta as posições; não usa OpenGL |
| `Md2Animacao` | uma animação (nome, quadro inicial e final); `montarLista` agrupa os quadros pelo nome |
| `Animador` | tempo, animação atual, velocidade, pausa e interpolação; não usa OpenGL |
| `Md2Model` | interpola dois quadros, mantém os VBOs e desenha (`extends Model`) |
| `Skins` | descobre as imagens da pasta `skins/` e escolhe a skin inicial |

`aula.Main` cria a janela, carrega modelo e texturas, trata o teclado e roda o laço de desenho.

## Créditos do modelo

O Ogro é de **Magarnigal**, com skins de **Ogro_Fix** e outros autores; os créditos completos
estão em `src/main/resources/md2/ogro/Ogro.txt`. Os arquivos foram obtidos do repositório do
three.js: https://github.com/mrdoob/three.js/tree/dev/examples/models/md2/ogro

## Sobre o código do OBJ

As classes do parser OBJ (`ObjModel`, `Face3D` etc.) continuam no projeto, mas o `Main` não as usa mais.

Perfis Maven para natives: Windows x64/ARM64, Linux x64/ARM64 e macOS Intel/Apple Silicon.
