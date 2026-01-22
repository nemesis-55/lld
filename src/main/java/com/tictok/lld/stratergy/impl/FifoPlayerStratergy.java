package com.tictok.lld.stratergy.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tictok.lld.models.Player;
import com.tictok.lld.stratergy.IPlayerSelectionStratergy;

public class FifoPlayerStratergy implements IPlayerSelectionStratergy {
    @Override
    public Optional<Player> selectPlayer(final Set<Player> players, final Optional<Player> currentPlayer) {
        if (players == null || players.isEmpty()) {
            return Optional.empty();
        }
        List<Player> playerList = players.stream().toList();
        if (currentPlayer == null || currentPlayer.isEmpty()) {
            return Optional.of(playerList.get(0));
        }
        int idx = playerList.indexOf(currentPlayer.get());
        int nextIdx = (idx + 1) % playerList.size();
        return Optional.of(playerList.get(nextIdx));
    }

}
