# DBA · Desarrollo Basado en Agentes

Prácticas de la asignatura Desarrollo Basado en Agentes del Grado en Ingeniería Informática de la Universidad de Granada, hechas en Java con JADE, una plataforma para crear agentes que se comunican entre ellos mediante mensajes.

## Las prácticas

**P2 · Movimiento de un agente por un mundo bidimensional.** El agente tiene que llegar desde su posición hasta un objetivo dentro de un mapa con obstáculos, sin conocer el mapa de antemano, así que solo sabe lo que perciben sus sensores a su alrededor. Funciona en un ciclo de ver, pensar y actuar, gastando energía en cada paso y recordando las casillas que ya ha visto y visitado. Si el camino hacia el objetivo está despejado, va directo hacia él. Si se encuentra un obstáculo, cambia a seguir la pared, inspirado en el *Bug Algorithm*, y la rodea hasta que puede volver a ir en línea recta. Además, elige por qué lado rodear la pared según dónde esté el objetivo, cambia de lado si detecta que se ha quedado dando vueltas en bucle, y evita dar giros de 180° bruscos. El recorrido se va mostrando en una ventana con el mapa en forma de matriz, donde se ven las casillas vistas y visitadas, y un panel con los controles y la información del agente. Hay mapas de todo tipo en `maps/`: sin obstáculos, con muros horizontales, verticales o en diagonal, y con obstáculos más complicados.

**P3 · Rescate de los renos de Papá Noel.** Sobre el mismo mundo, ahora hay varios agentes que tienen que hablar entre ellos. El agente principal quiere ayudar a Santa a encontrar a sus renos perdidos, pero Santa solo entiende mensajes en un idioma concreto, así que primero tiene que pedirle al elfo traductor que traduzca su propuesta. Si Santa la acepta, Rudolph le va dando al agente las coordenadas del siguiente reno, el agente va hasta él, y así hasta que ya no quedan renos. Para ello, los agentes usan los distintos tipos de mensaje de JADE (proponer, aceptar, rechazar, preguntar, informar...), y todas las conversaciones se registran en la interfaz.

## Documentación

- **[grupo_7_dba_p2_memoria.pdf](grupo_7_dba_p2_memoria.pdf)** · La memoria de la P2. Explica cómo está organizado el programa como un modelo-vista-controlador: el mundo solo guarda el mapa, el entorno guarda todo lo del agente (posición, objetivo, energía y memoria) y el agente solo puede ver el mundo a través de sus sensores. Incluye el diagrama de clases, las decisiones de diseño, cómo funciona el ciclo de ver, pensar y actuar, los dos modos de movimiento (directo y siguiendo la pared) con sus heurísticas, y una conclusión con las ventajas y las limitaciones del agente.

## Cómo ejecutarlo

**Requisitos:** JDK 17 o superior. La librería de JADE ya viene en `lib/jade/lib/jade.jar`.

La forma más cómoda es abrir el proyecto con NetBeans y ejecutar `src/Launcher.java`. Desde la terminal:

```bash
javac -cp lib/jade/lib/jade.jar -d out $(find src -name "*.java")
java -cp out:lib/jade/lib/jade.jar Launcher
```

(En Windows, cambia `:` por `;` en el `-cp`.)

El programa crea el contenedor de JADE, lanza los agentes (el agente principal, el elfo, Santa y Rudolph) y abre la ventana con el mapa.

## Estructura

```
src/
├── Launcher.java          <- crea el mundo, la ventana y lanza los agentes
├── model/                 <- el mundo, la percepción y el agente principal (AgentP3)
├── santa/, rudolph/, elfo/ <- los agentes de la P3
├── controller/            <- controlador y registro de mensajes
└── view/                  <- ventana con el mapa y el panel de información
model/AgentP2.java         <- agente de la P2
maps/                      <- mapas de prueba
lib/jade/                  <- librería JADE
```

## Equipo (Grupo 7)

- Adam Navarro Megías
- Fernando Lanzarot Facio
- Juan Ramón Gallardo Casado
- Antonio García Torres

Copia del repositorio original [Adman23/Practica2DBA](https://github.com/Adman23/Practica2DBA).
