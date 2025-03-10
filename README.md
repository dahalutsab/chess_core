# Chess Engine

![Chess](https://img.shields.io/badge/Game-Chess-blue)
![Spring Boot](https://img.shields.io/badge/Framework-Spring%20Boot-green)
![Java](https://img.shields.io/badge/Language-Java-orange)

A high-performance chess engine and bot built with **Spring Boot**. This project provides REST APIs for playing chess, generating moves, and analyzing positions. It includes features like move validation, FEN parsing, and advanced search algorithms (Minimax, Alpha-Beta pruning). The engine supports both human vs. bot and bot vs. bot gameplay, with plans to integrate machine learning for self-improvement.

---

## Features

- **Move Generation**: Generate legal moves for any chess position.
- **FEN Parsing**: Parse and generate FEN strings for board states.
- **Search Algorithms**: Implement Minimax and Alpha-Beta pruning for decision-making.
- **REST APIs**: Play against the bot or analyze positions via HTTP endpoints.
- **Extensible**: Designed for scalability and future integration with machine learning.

---

## Getting Started

### Prerequisites

- **Java 17+**: Ensure you have Java installed.
- **Maven**: For building the project.
- **PostgreSQL** (optional): For saving game states.

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/dahalutsab/chess_core.git
