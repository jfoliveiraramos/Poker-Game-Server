package poker.connection.protocol.message;

public enum Status {
    REQUEST("REQUEST"),
    OK("OK"),
    ERROR("ERROR");

    final String value;

    Status(String value) {
        this.value = value;
    }

    public Boolean equals(Status status) {
        return this.value.equals(status.value);
    }
}
