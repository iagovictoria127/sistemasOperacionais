package MemoryManager;

/**
 * Interface que representa um gerente de memória com seus métodos obrigatórios
 */
public interface IMemoryManager {
    public int alocate(int numOfWords);
    public void dealocate(int partition);
    public int translateLogicalIndexToFisical(int partition, int logicalIndex);
}
