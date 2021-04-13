import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public interface ReadInput {
    String readInput(int maxLength) throws TimeoutException, IllegalStateException, SocketException;

    default String readInput() throws TimeoutException, IllegalStateException, SocketException {
        return readInput(-1);
    }
}
