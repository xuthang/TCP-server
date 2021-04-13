import java.util.Optional;
import java.util.concurrent.TimeoutException;

public interface ReadInput {
    String readInput() throws TimeoutException;
}
