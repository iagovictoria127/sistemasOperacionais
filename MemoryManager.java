public class MemoryManager {
    
    int fisicalMemorySize;
    int partitionSize;
    int numOfPartitions;
    boolean[] logicalMemory;
    
    /**
     * Inicializa a partir dos parâmetros. Número de partições é calculado como: M/P, arredondado para cima
     * @param m Quantidade de palavras na memória
     * @param p Tamanho de cada partição
     */
    public MemoryManager(int m, int p) {
        fisicalMemorySize = m;
        partitionSize = p;
        numOfPartitions = (int) Math.ceil(m/p);
        logicalMemory = new boolean[numOfPartitions];
    }

    /** 
     * Aloca número de palavras em uma partição, e retorna onde foi salvo
     * @param numOfWords Quantidade de palavras do programa para alocar
     * @return índice da partição alocada, ou -1 se não conseguir alocar 
    */
    public int alocate(int numOfWords) {
        if(numOfWords > partitionSize) {
            return -1;
        } 

        for(int i = 0; i < logicalMemory.length; i++) {
            boolean isFrameOccupied = logicalMemory[i];
            if (isFrameOccupied) continue;
            logicalMemory[i] = true;
            return i;
        }

        return -1;
    }

     /** 
     * Desaloca uma partição da memória lógica
     * @param partition índice da partição a ser desalocada
    */
    public void dealocate(int partition) {
        if (partition > 0 && partition < logicalMemory.length) {
            logicalMemory[partition] = false;
        }
    }

    /**
     * Traduz endereço lógico de uma partição para físico
     * @param partition índice da partição a ser usada
     * @param logicalIndex endereço lógico da posição dentro da partição
     * @return endereço físico a ser acessado, ou -1 caso os parâmetros sejam inválidos
     */
    public int translateLogicalIndexToFisical(int partition, int logicalIndex) {
        if (logicalIndex < 0 || logicalIndex >= partitionSize) return -1;
        return (partition * partitionSize) + logicalIndex;
    }
}
