package log4j.layout.bunyan;

import org.apache.logging.log4j.message.Message;

public class FakeMessage implements Message {
    private static final long serialVersionUID = -81445503655660932L;

    private final String message;
    private final Throwable throwable;

    public FakeMessage(final String message, final Throwable throwable) {
        this.message = message;
        this.throwable = throwable;
    }

    @Override
    public String getFormattedMessage() {
        return message;
    }

    @Override
    public String getFormat() {
        return "";
    }

    @Override
    public Object[] getParameters() {
        return new Object[0];
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }
}
