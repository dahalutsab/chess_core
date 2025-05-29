/**
 * Author: Utsab Dahal
 * User:LEGION
 * Date:3/20/2025
 * Time:3:14 PM
 */

package com.utsab.chess_engine.decision.ai.minimax;

import com.utsab.chess_engine.decision.board.Move;

public record MoveResult(Move move, int score) {}