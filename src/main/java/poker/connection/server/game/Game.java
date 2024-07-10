package poker.connection.server.game;

import poker.Server;
import poker.connection.protocol.Connection;
import poker.connection.protocol.channels.ServerChannel;
import poker.connection.protocol.exceptions.ChannelException;
import poker.connection.protocol.exceptions.ClosedConnectionException;
import poker.connection.protocol.exceptions.RequestTimeoutException;
import poker.connection.protocol.message.Message;
import poker.connection.utils.VirtualThread;
import poker.game.common.GameState;
import poker.game.common.PokerPlayer;
import poker.game.server.Poker;

import java.util.ArrayList;

public class Game extends VirtualThread {
    private final Server server;
    private final ArrayList<Connection> playerConnections;
    private final Poker poker;

    public Game(Server server, ArrayList<Connection> playerConnections) {
        this.server = server;
        this.playerConnections = playerConnections;
        ArrayList<String> playerUsernames = new ArrayList<>();
        for (Connection connection : playerConnections) {
            playerUsernames.add(connection.getUsername());
        }
        poker = new Poker(playerUsernames);
    }

    public boolean reconnectPlayer(Connection newConnection) {
        int index = -1;

        for (int i = 0; i < playerConnections.size(); i++) {
            if (playerConnections.get(i).getUsername().equals(newConnection.getUsername())) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return false;
        }
        ServerChannel channel = playerConnections.get(index).getChannel();
        try {
            channel.requestConnectionEnd("Another connection was found for your account");
        } catch (ChannelException e) {
            server.log("Error while disconnecting old connection for player " + newConnection.getUsername());
        }
        playerConnections.set(index, newConnection);
        try {
            newConnection.getChannel().sendGameState(poker.getGameStateToSend(index));
        } catch (ClosedConnectionException e) {
            return false;
        }
        return true;
    }

    private void sendGameState() {
        for (int i = 0; i < playerConnections.size(); i++) {
            sendGameState(i);
        }
    }

    private void sendGameState(int player) {
        ServerChannel channel = playerConnections.get(player).getChannel();
        GameState gameState = poker.getGameStateToSend(player);

        try {
            channel.sendGameState(gameState);
        } catch (ClosedConnectionException e) {
            server.log("Player " + player + " disconnected while sending game state");
        }
    }

    private void notifyPlayers() {
        for (Connection connection : playerConnections) {
            try {
                connection.getChannel().notifyGameStart();
            } catch (ChannelException e) {
                server.log("Player " + connection.getUsername() + " disconnected while notifying game start");
            }
        }
    }

    @Override
    protected void run() {

        notifyPlayers();
        play();
        server.log("Game finished");

        if (server.isRankedMode()) {
            updateRanks();
        }
        finishGame();
    }

    private void play() {
        while (!poker.getIsGameOver()) {
            while (!poker.getIsHandOver()) {
                if (this.isInterrupted()) {
                    return;
                }
                int currentPlayer = poker.getCurrPlayer();
                sendGameState();
                makePlay(currentPlayer);
            }
            sendGameState();
            poker.endHand();
        }
        sendGameState();
    }

    private void makePlay(int player) {
        ServerChannel channel = playerConnections.get(player).getChannel();

        try {
            Message message = channel.getPlayerMove("It's your turn", poker.getGameStateToSend(player), 30);
            String action = message.getAttribute("action");
            Integer amount = message.getIntAttribute("amount");

            if (action == null || amount == null) {
                throw new RuntimeException(String.format(
                        "Invalid player move received from player %d - action: %s, amount: %d",
                        player,
                        action,
                        amount
                ));
            }
            PokerPlayer.PLAYER_ACTION playerAction = PokerPlayer.PLAYER_ACTION.fromString(action);
            poker.takeAction(playerAction, amount);
        } catch (RequestTimeoutException e) {
            try {
                channel.requestConnectionEnd("Player timed out while playing. May reconnect to continue");
            } catch (ClosedConnectionException ignored) {}
            server.log("Player " + playerConnections.get(player).getUsername() + " timed out. FOLDING");
            poker.takeAction(PokerPlayer.PLAYER_ACTION.FOLD, 0);
        } catch (ChannelException e) {
            server.log("Player " + playerConnections.get(player).getUsername() + " is disconnected. FOLDING");
            poker.takeAction(PokerPlayer.PLAYER_ACTION.FOLD, 0);
        }
    }

    private void finishGame() {
        for (Connection connection : playerConnections) {
            server.getQueuer().removePlayerFromRoom(connection);
        }
        server.getQueuer().requeuePlayers(playerConnections);
    }

    private void updateRanks() {
        server.log("Updating rankings");
        for (PokerPlayer player : poker.getGameWinners()) {
            Connection connection = null;
            for (Connection c : playerConnections) {
                if (c.getUsername().equals(player.getUsername())) {
                    connection = c;
                    break;
                }
            }
            if (connection != null) {
                server.getDatabase().updateRank(connection.getUsername(), player.getMoney() / 100);
                connection.setRank(server.getDatabase().getUserRank(connection.getUsername()));
            }
        }
    }
}
