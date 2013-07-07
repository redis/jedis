package redis.clients.jedis;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.exceptions.JedisException;

public class TransactionBlockList extends TransactionBlock
{
    private List<TransactionBlock> transactionBlocks = new ArrayList<TransactionBlock>();

    public TransactionBlockList(final Client client)
    {
        super(client);
    }

    public TransactionBlockList()
    {
    }

    public void add(final TransactionBlock transactionBlock)
    {
        if (transactionBlock != null) {
            transactionBlocks.add(transactionBlock);
        }
    }

    @Override
    public void execute() throws JedisException {
        for (TransactionBlock transactionBlock : transactionBlocks) {
            transactionBlock.setClient(client);
            transactionBlock.execute();
            pipelinedResponses.addAll(transactionBlock.pipelinedResponses);
        }
    }
}
