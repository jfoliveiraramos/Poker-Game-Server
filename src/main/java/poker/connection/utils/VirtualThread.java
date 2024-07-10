package poker.connection.utils;

public abstract class VirtualThread {

    Thread thread;
    public void start() {
        thread = Thread.startVirtualThread(this::run);
    }

    protected abstract void run();

    public void interrupt() {
        thread.interrupt();
    }

    public boolean isInterrupted() {
        return thread.isInterrupted();
    }
}
