package poker.client.state;

import com.google.gson.Gson;
import poker.connection.protocol.channels.ClientChannel;
import poker.connection.protocol.exceptions.ChannelException;
import poker.connection.protocol.exceptions.ClosedConnectionException;
import poker.connection.protocol.exceptions.UnexpectedMessageException;
import poker.connection.protocol.message.Message;
import poker.game.client.PokerClientGUI;
import poker.game.common.GameState;
import poker.utils.Pair;
import poker.utils.UserInput;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static poker.connection.protocol.message.State.MATCHMAKING;

public class Match extends ClientState {

    PokerClientGUI gui = new PokerClientGUI();

    public Match(ClientChannel channel) {
        super(channel);
    }

    @Override
    public ClientState handle() {
        try {
            Message message = channel.getRequest();
            return parseMessage(message);
        } catch (ClosedConnectionException e) {
            System.out.println("Connection to the server was lost.\n" + e.getMessage());
            return null;
        } catch (ChannelException e) {
            System.out.println("Error communicating with the server:\n" + e.getMessage());
            return null;
        }
    }

    private ClientState parseMessage(Message message) {

        switch (message.getState()) {
            case MATCH_DISPLAY -> {
                return handleMatchDisplay(message);
            }
            case MATCH_PLAY -> {
                return handleMatchPlay(message);
            }
            case REQUEUE -> {
                return handleRequeue();
            }
            default -> {
                System.out.println("Unexpected message received: " + message);
                return null;
            }
        }
    }

    private ClientState handleMatchDisplay(Message message) {
        String gameStateJson = message.getAttribute("gameState");
        GameState gameState = new Gson().fromJson(gameStateJson, GameState.class);
        gui.display(gameState);
        return this;
    }

    private ClientState handleMatchPlay(Message message) {

        String gameStateJson = message.getAttribute("gameState");
        GameState gameState = new Gson().fromJson(gameStateJson, GameState.class);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<Pair<String, Integer>> future = executor.submit(
                () -> gui.askMove(gameState)
        );

        final AtomicReference<Message> messageWrapper = new AtomicReference<>();
        Thread timer = Thread.ofVirtual().start(
                () -> {
                    try {
                        Message incoming = channel.getRequest();
                        messageWrapper.set(incoming);
                    } catch (ClosedConnectionException e) {
                        System.out.println("Connection to the server was lost.\n" + e.getMessage());
                        messageWrapper.set(null);
                    } catch (ChannelException e) {
                        System.out.println("Error communicating with the server:\n" + e.getMessage());
                        messageWrapper.set(null);
                    } finally {
                        future.cancel(true);
                    }
                }
        );

        Pair<String, Integer> action;

        try {
            action = future.get();
        } catch (CancellationException e) {
            if (messageWrapper.get() == null) {
                return null;
            }
            System.out.println("Timeout. You didn't make a move in time.");
            return null;
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Error getting user input:\n" + e.getMessage());
            return null;
        }

        try {
            channel.sendPlayerMove(action.getFirst(), action.getSecond().toString());
        } catch (Exception e) {
            System.out.println("Error communicating with the server:\n" + e.getMessage());
            return null;
        }

        try {
            timer.join();
            return parseMessage(messageWrapper.get());
        } catch (InterruptedException e) {
            System.out.println("Error handling message:\n" + e.getMessage());
            return null;
        }
    }

    private ClientState handleRequeue() {

        System.out.println("Do you want to requeue? (Y/N)");
        do {
            String response = new UserInput().nextLine();
            try {
                if (response.equalsIgnoreCase("Y")) {
                    channel.sendRequeueResponse(true);
                    Message matchmakingMessage = channel.getRequest();
                    if (matchmakingMessage.getState().equals(MATCHMAKING)) {
                        channel.acceptMatchmaking();
                        return new Matchmaking(channel);
                    } else {
                        throw new UnexpectedMessageException("Unexpected message received after requeueing: " + matchmakingMessage);
                    }
                } else if (response.equalsIgnoreCase("N")) {
                    channel.sendRequeueResponse(false);
                    return null;
                } else {
                    System.out.println("Invalid input. Please enter Y or N.");
                }
            } catch (ClosedConnectionException e) {
                System.out.println("Connection to the server was lost.\n" + e.getMessage());
                return null;
            } catch (ChannelException e) {
                System.out.println("Error communicating with the server:\n" + e.getMessage());
                return null;
            }
        } while (true);
    }
}
