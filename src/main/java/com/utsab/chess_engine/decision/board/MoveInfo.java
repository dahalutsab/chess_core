package com.utsab.chess_engine.decision.board;

public record MoveInfo(int movingPieceIndex, int fromSquare, int toSquare, int capturedPieceIndex, boolean previousWhiteToMove) {}