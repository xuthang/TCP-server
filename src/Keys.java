public class Keys {
    private final int[] serverKeys = {23019, 32037, 18789, 16443, 18189};
    private final int[] clientKeys = {32037, 29295, 13603, 29533, 21952};

    int getSize()
    {
        return Math.min(serverKeys.length, clientKeys.length);
    }

    int getServerKey(int ID)
    {
        return serverKeys[ID];
    }

    int getClientKey(int ID)
    {
        return clientKeys[ID];
    }
}
