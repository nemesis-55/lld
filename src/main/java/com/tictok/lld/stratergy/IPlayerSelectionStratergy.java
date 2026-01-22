package com.tictok.lld.stratergy;

import java.util.Optional;
import java.util.Set;

import com.tictok.lld.models.Player;

public interface IPlayerSelectionStratergy {
    Optional<Player> selectPlayer(Set<Player> players, Optional<Player> currentPlayer);
}
