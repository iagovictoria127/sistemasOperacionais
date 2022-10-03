// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais
//
// VM
//    HW = memória, cpu
//    SW = tratamento int e chamada de sistema
// Funcionalidades de carga, execução e dump de memória

import java.util.*;

public class SistemaPag {
	
	// -------------------------------------------------------------------------------------------------------
	// --------------------- H A R D W A R E - definicoes de HW ---------------------------------------------- 

	// -------------------------------------------------------------------------------------------------------
	// --------------------- M E M O R I A -  definicoes de palavra de memoria, memória ---------------------- 
	private int limite_overflow = 10000;

	public class Memory {
		public int tamMem;    
        public Word[] m;                  // m representa a memória fisica:   um array de posicoes de memoria (word)
	
		public Memory(int size){
			tamMem = size;
		    m = new Word[tamMem];      
		    for (int i=0; i<tamMem; i++) { m[i] = new Word(Opcode.___,-1,-1,-1); };
		}
		
		public void dump(Word w) {        // funcoes de DUMP nao existem em hardware - colocadas aqui para facilidade
						System.out.print("[ "); 
						System.out.print(w.opc); System.out.print(", ");
						System.out.print(w.r1);  System.out.print(", ");
						System.out.print(w.r2);  System.out.print(", ");
						System.out.print(w.p);  System.out.println("  ] ");
		}
		public void dump(int ini, int fim) {
			for (int i = ini; i < fim; i++) {		
				System.out.print(i); System.out.print(":  ");  dump(m[i]);
			}
		}
    }
	
	//GERENTE DE MEMÓRIA
	public class MemoryManager{
    
		int maxMemorySize;
		int pageSize;
		int numOfPages;
		int numOfAvailablePages;
		boolean[] logicalMemory;
		
		/**
		 * Inicializa a partir dos parâmetros. Número de partições é calculado como: M/P, arredondado para cima
		 * @param m Tamanho máximo da memória
		 * @param p Tamanho de página
		 */
		public MemoryManager(int m, int p) {
			maxMemorySize = m;
			pageSize = p;
			numOfPages = (int) Math.ceil(m/p);
			numOfAvailablePages = numOfPages;
			logicalMemory = new boolean[numOfPages];
		}
		
		/**
		 * Verifica se é possível alocar o programa na memória
		 * @param numOfWords numero de palavras do programa
		 * @return true se possível, false caso contrário
		*/
		public boolean allocable(int numOfWords) {
			int numOfPagesForProccess = (int) Math.ceil(numOfWords / pageSize);
			return numOfPagesForProccess <= numOfAvailablePages;
		}

		/** 
		 * Aloca número de palavras em páginas, e retorna onde foi salvo
		 * @param numOfWords Quantidade de palavras do programa para alocar
		 * @return índices das páginas alocadas, ou [-1] no índice 0 se não conseguir alocar 
		*/
		public int[] alocate(int numOfWords) {	
			if(!allocable(numOfWords)) {
				int[] dullArray = { -1 };
				return dullArray;
			} 
			int numOfPagesForProccess = 0;
			if(numOfWords % pageSize == 0){
				numOfPagesForProccess = (numOfWords / pageSize);
			} else {
				numOfPagesForProccess = (numOfWords / pageSize) + 1;
			}
			int[] pagesIndex = new int[numOfPagesForProccess];
			int pagesCount = 0;
	
			for(int i = 0; i < logicalMemory.length; i++) {
				if(numOfPagesForProccess <= 0) { break; }
				
				boolean isPageOccupied = logicalMemory[i];
				if (isPageOccupied) { continue; }

				logicalMemory[i] = true;
				pagesIndex[pagesCount] = i;
				System.out.println(pagesCount);
				pagesCount++;
				numOfAvailablePages--;
				numOfPagesForProccess--;
			}
			return pagesIndex;
		}
	
		 /** 
		 * Desaloca páginas da memóra lógica
		 * @param pages índices das páginas a ser desalocadas
		*/
		public void dealocate(int[] pages) {
			for (int page : pages) {
				if(page < 0 || page >= logicalMemory.length) { continue; }
				logicalMemory[page] = false;
				numOfAvailablePages++;
			}
		}
	
		/**
		 * Traduz endereço lógico de uma partição para físico
		 * @param index índice da page a ser usada
		 * @param offset endereço lógico da posição dentro da page
		 * @return endereço físico a ser acessado, ou -1 caso os parâmetros sejam inválidos
		 */
		public int translateLogicalIndexToFisical(int index, int offset) {
			if (index < 0 || offset < 0 || index >= logicalMemory.length) { return -1; }
			
			return (index * pageSize) + offset;
		}
	}
	


    // -------------------------------------------------------------------------------------------------------

	public class Word { 	// cada posicao da memoria tem uma instrucao (ou um dado)
		public Opcode opc; 	//
		public int r1; 		// indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
		public int r2; 		// indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
		public int p; 		// parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

		public Word(Opcode _opc, int _r1, int _r2, int _p) {  // vide definição da VM - colunas vermelhas da tabela
			opc = _opc;   r1 = _r1;    r2 = _r2;	p = _p;
		}
	}
	
	// -------------------------------------------------------------------------------------------------------
    // --------------------- C P U  -  definicoes da CPU ----------------------------------------------------- 

	public enum Opcode {
		DATA, ___,		                    // se memoria nesta posicao tem um dado, usa DATA, se nao usada ee NULO ___
		JMP, JMPI, JMPIG, JMPIL, JMPIE,     // desvios e parada
		JMPIM, JMPIGM, JMPILM, JMPIEM, STOP, 
		JMPIGK, JMPILK, JMPIEK, JMPIGT,     
		ADDI, SUBI, ADD, SUB, MULT,         // matematicos
		LDI, LDD, STD, LDX, STX, MOVE,      // movimentacao
        TRAP                                // chamada de sistema
	}

	public enum Interrupts {               // possiveis interrupcoes que esta CPU gera
		noInterrupt, intEnderecoInvalido, intInstrucaoInvalida, intOverflow, intSTOP, intTrap;
	}

	public class CPU {
		private int maxInt; // valores maximo e minimo para inteiros nesta cpu
		private int minInt;
		private int indexpart;			// característica do processador: contexto da CPU ...
		private int pc; 			// ... composto de program counter,
		private Word ir; 			// instruction register,
		private int[] reg; 
		private int[] pag;      	// registradores da CPU
		private Interrupts irpt; 	// durante instrucao, interrupcao pode ser sinalizada
		private int base;   		// base e limite de acesso na memoria
		private int limite; // por enquanto toda memoria pode ser acessada pelo processo rodando
							// ATE AQUI: contexto da CPU - tudo que precisa sobre o estado de um processo para executa-lo
							// nas proximas versoes isto pode modificar

		private Memory mem;               // mem tem funcoes de dump e o array m de memória 'fisica' 
		private Word[] m;                 // CPU acessa MEMORIA, guarda referencia a 'm'. m nao muda. semre será um array de palavras

		private InterruptHandling ih;     // significa desvio para rotinas de tratamento de  Int - se int ligada, desvia
        private SysCallHandling sysCall;  // significa desvio para tratamento de chamadas de sistema - trap 
		private boolean debug;            // se true entao mostra cada instrucao em execucao
						
		public CPU(Memory _mem, InterruptHandling _ih, SysCallHandling _sysCall, boolean _debug) {     // ref a MEMORIA e interrupt handler passada na criacao da CPU
			maxInt =  32767;        // capacidade de representacao modelada
			minInt = -32767;        // se exceder deve gerar interrupcao de overflow
			mem = _mem;	            // usa mem para acessar funcoes auxiliares (dump)
			m = mem.m; 				// usa o atributo 'm' para acessar a memoria.
			reg = new int[10]; 		// aloca o espaço dos registradores - regs 8 e 9 usados somente para IO
			ih = _ih;               // aponta para rotinas de tratamento de int
            sysCall = _sysCall;     // aponta para rotinas de tratamento de chamadas de sistema
			debug =  _debug;        // se true, print da instrucao em execucao
		}
		
		/* 
		private boolean legal(int e) {                             // todo acesso a memoria tem que ser verificado
			// ????
			return true;
		}
		*/

		// teste se houve overflow
		private boolean testOverflow(int v) {                       // toda operacao matematica deve avaliar se ocorre overflow                      
			if (v < limite_overflow) {                              
				return false;
			};
			irpt = Interrupts.intOverflow;
			//System.out.println("Interrupção: Overflow");
			return true;
		}

		// testa se o endereco e invalido
		/*private boolean legal(int v){
			int endP = mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[0], v);
			int x;
			int runningLen = pm.running.get(0).memAlo.length;
			for(x = 0; x < runningLen; x++){
				int endLimit = (mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[x], 0)) + mm.pageSize;
				if( endP <= endLimit && endP >= (endLimit - mm.pageSize) ){
					System.out.println(pm.running.get(0).memAlo[x]);
				System.out.println(pm.running.get(0).memAlo.length);
				System.out.println(endP);
				System.out.println(endLimit);
					return true;
				}
				//System.out.println(mm.pageSize);
			}
			irpt = Interrupts.intEnderecoInvalido;

			return false;
		}*/

		private boolean legal(int v){
			int endP = mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[0], v%mm.pageSize);
			int pagLength = pm.running.get(0).memAlo.length;
			//System.out.println(mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[0], 0)+"Test End");
			int endLimit = mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[0], 0) + mm.pageSize*pagLength;
			if( endP > endLimit ){
				irpt = Interrupts.intEnderecoInvalido;
				//System.out.println("Interrupção: Endereço Inválido");
				return false;
			}
			//System.out.println(endP);
			//System.out.println(endLimit);
			return true;
		}

		// testa se a instrucao e valida
		private boolean testInstrucaoInv(Opcode v){
			for(Opcode p: Opcode.values()){
				if(p == v){
				return false;
				}
			}
			//System.out.println("Interrupção: Instrução Inválida");
			irpt = Interrupts.intInstrucaoInvalida; 
			return true;
		}
		
		// testa se o programa parou
		private boolean testParada(Opcode v){
			if(v == ir.opc.STOP){

				//System.out.println("Interrupção: Stop");
				irpt = Interrupts.intSTOP;
				return true;
			}
			return false;
		}

		public void setContext(int _base, int _limite, int[] _pag, int _indexpart, int _reg[]) {  // no futuro esta funcao vai ter que ser 
			base = _base;                                          // expandida para setar todo contexto de execucao,
			limite = _limite;									   // agora,  setamos somente os registradores base,
			pc = mm.translateLogicalIndexToFisical(_pag[0], 0);  
			pag = _pag;                                           // limite e pc (deve ser zero nesta versao)
			irpt = Interrupts.noInterrupt; 
			indexpart = _indexpart; 
			reg = _reg;                     // reset da interrupcao registrada  
		}
		
		public void run() { 		// execucao da CPU supoe que o contexto da CPU, vide acima, esta devidamente setado			
			while (true) { 			// ciclo de instrucoes. acaba cfe instrucao, veja cada caso.
			   // --------------------------------------------------------------------------------------------------
			   // FETCH
			   
				if (legal(pc)) { 	// pc valido
					ir = m[pc]; 	// <<<<<<<<<<<<           busca posicao da memoria apontada por pc, guarda em ir
					pm.running.get(0).programCounter = pc;
					if (debug) { System.out.print("                               pc: "+pc+"       exec: ");  mem.dump(ir); }
			   // --------------------------------------------------------------------------------------------------
			   // EXECUTA INSTRUCAO NO ir
					switch (ir.opc) {   // conforme o opcode (código de operação) executa

					// Instrucoes de Busca e Armazenamento em Memoria
					    case LDI: // Rd ← k
							reg[ir.r1] = ir.p;
							pc++;

							break;

						case LDD: // Rd <- [A]
						    if (legal(ir.p)) {
							   reg[ir.r1] = m[ir.p].p;
							   pc++;
						    }
						    break;

						case LDX: // RD <- [RS] // NOVA
							if (legal(reg[ir.r2])) {
								reg[ir.r1] = m[reg[ir.r2]].p;
								pc++;
							}
							break;

						case STD: // [A] ← Rs
						    if (legal(ir.p)) {
							    m[(mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[0], ir.p))].opc = Opcode.DATA;
							    m[(mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[0], ir.p))].p = reg[ir.r1];
							    pc++;
								//mem.dump(16, 48);
							};
						    break;

						case STX: // [Rd] ←Rs
						    if (legal(reg[ir.r1])) {
							    m[(mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[0], reg[ir.r1]))].opc = Opcode.DATA;      
							    m[(mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[0], reg[ir.r1]))].p = reg[ir.r2];          
								pc++;
							};
							break;
						
						case MOVE: // RD <- RS
							reg[ir.r1] = reg[ir.r2];
							pc++;
							break;	
							
					// Instrucoes Aritmeticas
						case ADD: // Rd ← Rd + Rs
							reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
							testOverflow(reg[ir.r1]);
							pc++;
							break;

						case ADDI: // Rd ← Rd + k
							reg[ir.r1] = reg[ir.r1] + ir.p;
							testOverflow(reg[ir.r1]);
							pc++;
							break;

						case SUB: // Rd ← Rd - Rs
							reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
							testOverflow(reg[ir.r1]);
							pc++;
							break;

						case SUBI: // RD <- RD - k // NOVA
							reg[ir.r1] = reg[ir.r1] - ir.p;
							testOverflow(reg[ir.r1]);
							pc++;
							break;

						case MULT: // Rd <- Rd * Rs
							reg[ir.r1] = reg[ir.r1] * reg[ir.r2];  
							testOverflow(reg[ir.r1]);
							pc++;
							break;

					// Instrucoes JUMP
						case JMP: // PC <- k
						pc = mm.translateLogicalIndexToFisical(indexpart, ir.p);
						break;
						
						case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
							if (reg[ir.r2] > 0) {
								pc = mm.translateLogicalIndexToFisical(indexpart,reg[ir.r1]);
							} else {
								pc++;
							}
							break;

						case JMPIGK: // If RC > 0 then PC <- k else PC++
							if (reg[ir.r2] > 0) {
								pc = mm.translateLogicalIndexToFisical(indexpart,ir.p);
							} else {
								pc++;
							}
							break;
	
						case JMPILK: // If RC < 0 then PC <- k else PC++
							 if (reg[ir.r2] < 0) {
								pc = mm.translateLogicalIndexToFisical(indexpart,ir.p);
							} else {
								pc++;
							}
							break;
	
						case JMPIEK: // If RC = 0 then PC <- k else PC++
								if (reg[ir.r2] == 0) {
									pc = mm.translateLogicalIndexToFisical(indexpart,ir.p);
								} else {
									pc++;
								}
							break;
	
	
						case JMPIL: // if Rc < 0 then PC <- Rs Else PC <- PC +1
								 if (reg[ir.r2] < 0) {
									pc = mm.translateLogicalIndexToFisical(indexpart,reg[ir.r1]);
								} else {
									pc++;
								}
							break;
		
						case JMPIE: // If Rc = 0 Then PC <- Rs Else PC <- PC +1
								 if (reg[ir.r2] == 0) {
									pc = mm.translateLogicalIndexToFisical(indexpart,reg[ir.r1]);
								} else {
									pc++;
								}
							break; 
	
						case JMPIM: // PC <- [A]
								 pc = mm.translateLogicalIndexToFisical(indexpart,m[ir.p].p);
							 break; 
	
						case JMPIGM: // If RC > 0 then PC <- [A] else PC++
								 if (reg[ir.r2] > 0) {
									pc = mm.translateLogicalIndexToFisical(indexpart,m[ir.p].p);
								} else {
									pc++;
								}
							 break;  
	
						case JMPILM: // If RC < 0 then PC <- k else PC++
								 if (reg[ir.r2] < 0) {
									pc = mm.translateLogicalIndexToFisical(indexpart,m[ir.p].p);
								} else {
									pc++;
								}
							 break; 
	
						case JMPIEM: // If RC = 0 then PC <- k else PC++
								if (reg[ir.r2] == 0) {
									pc = mm.translateLogicalIndexToFisical(indexpart,m[ir.p].p);
								} else {
									pc++;
								}
							 break; 
	
						case JMPIGT: // If RS>RC then PC <- k else PC++
								if (reg[ir.r1] > reg[ir.r2]) {
									pc = mm.translateLogicalIndexToFisical(indexpart, ir.p);
								} else {
									pc++;
								}
							 break; 

					// outras
						case STOP: // por enquanto, para execucao
						testParada(ir.opc);
							break;

						case DATA:
						testInstrucaoInv(ir.opc);
							break;

					// Chamada de sistema
					    case TRAP:
						     irpt = Interrupts.intTrap;
						     sysCall.handle();            // <<<<< aqui desvia para rotina de chamada de sistema, no momento so temos IO
							 pc++;
						     break;

					// Inexistente
						default:
						testInstrucaoInv(ir.opc);
							break;
					}
				}
			   // --------------------------------------------------------------------------------------------------
			   // VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
				if (!(irpt == Interrupts.noInterrupt  || irpt == Interrupts.intTrap)) {   // existe interrupção
					ih.handle(irpt,pc);                       // desvia para rotina de tratamento
					break; // break sai do loop da cpu
				}
			}  // FIM DO CICLO DE UMA INSTRUÇÃO
		}      
	}



    // ------------------ C P U - fim ------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

    
	
    // ------------------- V M  - constituida de CPU e MEMORIA -----------------------------------------------
    // -------------------------- atributos e construcao da VM -----------------------------------------------
	public class   VM {
		public int tamMem;    
        public Word[] m;  
		public Memory mem;   
        public CPU cpu;    

        public VM(InterruptHandling ih, SysCallHandling sysCall){   
		 // vm deve ser configurada com endereço de tratamento de interrupcoes e de chamadas de sistema
	     // cria memória
		     tamMem = 1024;
  		 	 mem = new Memory(tamMem);
			 m = mem.m;
	  	 // cria cpu
			 cpu = new CPU(mem,ih,sysCall, true);                   // true liga debug
	    }	
	}
    // ------------------- V M  - fim ------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

    // --------------------H A R D W A R E - fim -------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	
	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// ------------------- S O F T W A R E - inicio ----------------------------------------------------------

	// ------------------- I N T E R R U P C O E S  - rotinas de tratamento ----------------------------------
    public class InterruptHandling {
            public void handle(Interrupts irpt, int pc) {   // apenas avisa - todas interrupcoes neste momento finalizam o programa
				System.out.println("                                               Interrupcao "+ irpt+ "   pc: "+pc);
				switch(irpt){
				case intSTOP:
				System.out.println("Interrupcao: O programa chegou ao fim");
				pm.deallocateProcess(pm.running.get(0).id);
				break;
				case intEnderecoInvalido:
				System.out.println("Interrupcao: Acesso a endereco de memoria invalido");
				pm.deallocateProcess(pm.running.get(0).id);
				break;
				case intInstrucaoInvalida: 
				System.out.println("Interrupcao: Instrucao de programa invalida");
				pm.deallocateProcess(pm.running.get(0).id);
				break;
				case intOverflow: 
				System.out.println("Interrupcao Overflow");
				pm.deallocateProcess(pm.running.get(0).id);
				break;
				}
			}
	}

    // ------------------- C H A M A D A S  D E  S I S T E M A  - rotinas de tratamento ----------------------
    public class SysCallHandling {
        private VM vm;
        public void setVM(VM _vm){
            vm = _vm;
        }
        public void handle() {   // apenas avisa - todas interrupcoes neste momento finalizam o programa
			pm.interrupted.add(pm.running.get(0));
			pm.running.remove(0);
			System.out.println("                                               Chamada de Sistema com op  /  par:  "+ vm.cpu.reg[8] + " / " + vm.cpu.reg[9]);
			if(vm.cpu.reg[8] == 1){
				int r9 = (mm.translateLogicalIndexToFisical(pm.interrupted.get(0).memAlo[0], 0)) + pm.interrupted.get(0).r[9];
				System.out.println("TRAP: Processo de id "+ pm.interrupted.get(0).id + " solicitando dados");
				System.out.println("Digite um numero inteiro: ");
				Scanner sc = new Scanner(System.in);
				int op = sc.nextInt();
				vm.m[r9].p = op;
				vm.cpu.irpt = Interrupts.noInterrupt;
				pm.running.add(pm.interrupted.get(0));
				pm.interrupted.remove(0);
			}
			else if(vm.cpu.reg[8] == 2){
				int r9 = (mm.translateLogicalIndexToFisical(pm.interrupted.get(0).memAlo[0], 0)) + pm.interrupted.get(0).r[9];
				System.out.println("TRAP: Mostrando na tela:");
				System.out.println(vm.m[r9].p);
				vm.cpu.irpt = Interrupts.noInterrupt;
				pm.running.add(pm.interrupted.get(0));
				pm.interrupted.remove(0);
			}
		}
    }

    // ------------------ U T I L I T A R I O S   D O   S I S T E M A -----------------------------------------
	// ------------------ load é invocado a partir de requisição do usuário

	private void loadProgram(Word[] p, Word[] m) {
		for (int i = 0; i < p.length; i++) {
			m[i].opc = p[i].opc;     m[i].r1 = p[i].r1;     m[i].r2 = p[i].r2;     m[i].p = p[i].p;
		}
	}

	private void loadProgram(Word[] p) {
		loadProgram(p, vm.m);
	}

	/*private void loadAndExec(Word[] p){
		loadProgram(p);    // carga do programa na memoria
				System.out.println("---------------------------------- programa carregado na memoria");
				vm.mem.dump(0, p.length);            // dump da memoria nestas posicoes				
		vm.cpu.setContext(0, vm.tamMem - 1, 0);      // seta estado da cpu ]
				System.out.println("---------------------------------- inicia execucao ");
		vm.cpu.run();                                // cpu roda programa ate parar	
				System.out.println("---------------------------------- memoria após execucao ");
				vm.mem.dump(0, p.length);            // dump da memoria com resultado
	}
	*/

	private void loadPrograms(Word [] p, int indexPart){
		int count = 0;
			for(int x = (mm.translateLogicalIndexToFisical(indexPart, count)); x < (mm.translateLogicalIndexToFisical(indexPart, p.length));x++){
				vm.m[x].opc = p[count].opc;
				vm.m[x].r1 = p[count].r1;
				vm.m[x].r2 = p[count].r2;
				vm.m[x].p = p[count].p;
				count++;
			}

	}

	private void cleanPartition(ProcessControlBlock pcb){
		int count = 0;
		int endIni = mm.translateLogicalIndexToFisical(pcb.memAlo[0] , count);
		for(int x = endIni; x < (endIni + (mm.pageSize*pcb.memAlo.length)); x++){
				vm.m[x].opc = Opcode.___;
				vm.m[x].r1 = -1;
				vm.m[x].r2 = -1;
				vm.m[x].p = -1;
				count++;
		}
	}

	private void exec(int id){
		ProcessControlBlock pcb = pm.searchProcess(id);
			if(pcb != null){
				int end = mm.translateLogicalIndexToFisical(pcb.memAlo[0], 0);
				//System.out.println("---------------------------------- programa carregado na memoria");
				//vm.mem.dump(end, end + pcb.memLimit);            // dump da memoria nestas posicoes				
		vm.cpu.setContext(0, vm.tamMem - 1, pcb.memAlo, pcb.memAlo[0], pcb.r);      // seta estado da cpu ]
				//System.out.println("---------------------------------- inicia execucao ");
		pm.ready.remove(pcb);
		pm.running.add(pcb);
		vm.cpu.run();                                // cpu roda programa ate parar	
				//System.out.println("---------------------------------- memoria apos execucao ");
				//vm.mem.dump(end, end + pcb.memLimit);            // dump da memoria com resultado
		} else {
			System.out.println("Processo nao encontrado");
		}
	}

	public void console(){
		int op = -1;
		int programs = -1;
		int prcs = -1;
		int memI = -1;
		int memF = -1;
		Scanner sc = new Scanner(System.in);
		while(op!=0){
				
				System.out.println("\n ---------------------------------");
				System.out.println("Seja bem vindo ao sistema!");
				System.out.println("Selecione a operacao desejada:");
				System.out.println("	1 - Criar processo");
				System.out.println("	2 - Dump processo");
				System.out.println("	3 - Desaloca processo");
				System.out.println("	4 - Dump Memória");
				System.out.println("	5 - Executar processo");
				System.out.println("	6 - TraceOn");
				System.out.println("	7 - TraceOff");
				System.out.println("	0 - Sair");
				System.out.print("Operacao: ");
				
				op = sc.nextInt();
		
			switch (op) {
						case 1:
							System.out.println("Selecione uma ação:");
							System.out.println("1- Fibonacci");
							System.out.println("2- ProgMinimo");
							System.out.println("3- Fatorial");
							System.out.println("4- FatorialTrap");
							System.out.println("5- FibonacciTrap");
							System.out.println("6- Bubble Sort");
							System.out.println("0- Voltar.");
								
								programs = sc.nextInt();
								switch(programs){
									case 1: pm.createProcess(progs.fibonacci10);
									break;

									case 2: pm.createProcess(progs.progMinimo);
									break;

									case 3: pm.createProcess(progs.fatorial);
									break;

									case 4: pm.createProcess(progs.fatorialTRAP);
									break;

									case 5: pm.createProcess(progs.fibonacciTRAP);
									break;

									case 0: 
									break;

									default: System.out.println("Opcao invalida");
									break;
								}
						
							break;
						case 2:
							System.out.println("Digite o número do processo: ");
							prcs = sc.nextInt();
							pm.dumpProcess(prcs);
							break;

						case 3:
							System.out.println("Digite o número do processo: ");
							prcs = sc.nextInt();
							pm.deallocateProcess(prcs);
							break;

						case 4:
							System.out.println("Digite a posicao de inicio da memoria: ");
							memI = sc.nextInt();
							System.out.println("Digite a posicao de fim da memoria: ");
							memF = sc.nextInt();
							vm.mem.dump(memI, memF+1);
							
							break;

						case 5:
							System.out.println("Digite o número do processo: ");
							prcs = sc.nextInt();
							exec(prcs);
							break;

						case 6:  System.out.println("Trace ativo");
							vm.cpu.debug = true;
							break;

						case 7:
							System.out.println("Trace desativado");
							vm.cpu.debug = false;
						break;

						case 0: System.exit(0); 
						break;    
			}
		}                              

	}


	// -------------------------------------------------------------------------------------------------------
    // -------------------  S I S T E M A --------------------------------------------------------------------

	public VM vm;
	public InterruptHandling ih;
	public SysCallHandling sysCall;
	public static Programas progs;
	public MemoryManager mm;
	public ProcessManager pm;

    public SistemaPag(){   // a VM com tratamento de interrupções
		 ih = new InterruptHandling();
         sysCall = new SysCallHandling();
		 vm = new VM(ih, sysCall);
		 sysCall.setVM(vm);
		 progs = new Programas();
		 mm = new MemoryManager(1024, 16);
		 pm = new ProcessManager();
	}

	// GERENTE DE PROCESSOS
	public class ProcessControlBlock{
		public int id;
		public int programCounter = 0;
		public int[] memAlo;
		public int memLimit;
		public int r[];

		public ProcessControlBlock(int id, int memAlo[], int memLimit){
			this.memAlo = memAlo;
			this.id = id;
			this.memLimit = memLimit;
			r = new int[10];
			programCounter = mm.translateLogicalIndexToFisical(memAlo[0], 0);
		}
	}

	public class ProcessManager{
		public ArrayList<ProcessControlBlock> pcbA;
		public ArrayList<ProcessControlBlock> running;
		public ArrayList<ProcessControlBlock> interrupted;
		public ArrayList<ProcessControlBlock> ready;
		public int id = 0;

		public ProcessManager(){
			pcbA = new ArrayList<ProcessControlBlock>();
			running = new ArrayList<ProcessControlBlock>();
			interrupted = new ArrayList<ProcessControlBlock>();
			ready = new ArrayList<ProcessControlBlock>();
		}


		public boolean createProcess(Word[] w){
			ProcessControlBlock pcb;
			if(mm.allocable(w.length)){
				int[] memA = mm.alocate(w.length);
				pcb = new ProcessControlBlock(id, memA, w.length);
				pcbA.add(pcb);
				ready.add(pcb);
				//running.add(pcb);
				loadPrograms(w, memA[0]);

			} else{
				System.out.println("Sem espaço na memoria");
				return false;
			}
			System.out.println("Processo criado com id: "+(id));
			id++;
			return true;
		}

		public void deallocateProcess(int id){
			for(ProcessControlBlock pcbs : pcbA){
				if(pcbs.id == id){
					mm.dealocate(pcbs.memAlo);
					pcbA.remove(pcbs);
					//ready.remove(pcbs.id);
					cleanPartition(pcbs);
					System.out.println("Processo com ID "+id+" desalocado");
					return;
				}
			}
			System.out.println("Processo noo existe");
		}

		public ProcessControlBlock searchProcess(int id){
			for(ProcessControlBlock pcbs : pcbA){
				if(pcbs.id == id){
					return pcbs;
				}
			}
			return null;
		}

		public void dumpProcess(int id){
			boolean cntrl = false;
			for(ProcessControlBlock pcbs : pcbA){
				if(pcbs.id == id){
				System.out.println("ID: "+ pcbs.id);
				System.out.println("Indice de pagina: "+ pcbs.memAlo[0]);
				System.out.println("PC: "+ pcbs.programCounter);
				vm.mem.dump(mm.translateLogicalIndexToFisical(pcbs.memAlo[0], 0), (mm.translateLogicalIndexToFisical(pcbs.memAlo[0], 0)) + pcbs.memLimit);
				cntrl = true;
				} 
			}
			if(!cntrl) System.out.println("Processo nao existe");
			}

	}

    // -------------------  S I S T E M A - fim --------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // ------------------- instancia e testa sistema
	public static void main(String args[]) {
		
		SistemaPag s = new SistemaPag();

		/*Scanner sc = new Scanner(System.in);
		
		System.out.println("Seja bem vindo ao sistema!");
		System.out.println("Selecione uma ação:");
		System.out.println("1-Fibonacci");
		System.out.println("2-ProgMinimo");
		System.out.println("3-Fatorial");
		System.out.println("4-FatorialTrap");
		System.out.println("5-FibonacciTrap");
		System.out.println("6-Bubble Sort");
		System.out.println("0-Exit.");

		String opcao = sc.next();
		sc.close();

		switch (opcao) {
			case "0":
				return;
			case "1":
				s.loadAndExec(progs.fibonacci10);
			case "2":
				s.loadAndExec(progs.progMinimo);
			case "3":
				s.loadAndExec(progs.fatorial);
			case "4":
				s.loadAndExec(progs.fatorialTRAP);
			case "5":
				s.loadAndExec(progs.fibonacciTRAP);		
			case "6":
				s.loadAndExec(progs.PC);
			default:
				break;
		} */

		//s.exec(progs.progMinimo);
		//s.exec(progs.fatorial);
		//s.exec(progs.fibonacci10);
		s.console();
		//s.pm.dumpProcess();
		//s.pm.createProcess(progs.progMinimo);
		//s.loadAndExec(progs.fibonacci10);
		//s.loadAndExec(progs.progMinimo);
		//s.loadAndExec(progs.fatorial);
		//s.loadAndExec(progs.fatorialTRAP); // saida
		//s.loadAndExec(progs.fibonacciTRAP); // entrada
		//s.loadAndExec(progs.PC); // bubble sort
			
	}


   // -------------------------------------------------------------------------------------------------------
   // -------------------------------------------------------------------------------------------------------
   // -------------------------------------------------------------------------------------------------------
   // --------------- P R O G R A M A S  - não fazem parte do sistema
   // esta classe representa programas armazenados (como se estivessem em disco) 
   // que podem ser carregados para a memória (load faz isto)

   public class Programas {
	   public Word[] fatorial = new Word[] {
	 	           // este fatorial so aceita valores positivos.   nao pode ser zero
	 											 // linha   coment
	 		new Word(Opcode.LDI, 0, -1, 4),      // 0   	r0 é valor a calcular fatorial
	 		new Word(Opcode.LDI, 1, -1, 1),      // 1   	r1 é 1 para multiplicar (por r0)
	 		new Word(Opcode.LDI, 6, -1, 1),      // 2   	r6 é 1 para ser o decremento
	 		new Word(Opcode.LDI, 7, -1, 8),      // 3   	r7 tem posicao de stop do programa = 8
	 		new Word(Opcode.JMPIE, 7, 0, 0),     // 4   	se r0=0 pula para r7(=8)
			new Word(Opcode.MULT, 1, 0, -1),     // 5   	r1 = r1 * r0
	 		new Word(Opcode.SUB, 0, 6, -1),      // 6   	decrementa r0 1 
	 		new Word(Opcode.JMP, -1, -1, 4),     // 7   	vai p posicao 4
	 		new Word(Opcode.STD, 1, -1, 10),     // 8   	coloca valor de r1 na posição 10
	 		new Word(Opcode.STOP, -1, -1, -1),   // 9   	stop
	 		new Word(Opcode.DATA, -1, -1, -1) }; // 10   ao final o valor do fatorial estará na posição 10 da memória                                    
		
	   public Word[] progMinimo = new Word[] {
		    new Word(Opcode.LDI, 0, -1, 999), 		
			new Word(Opcode.STD, 0, -1, 10), 
			new Word(Opcode.STD, 0, -1, 11), 
			new Word(Opcode.STD, 0, -1, 12), 
			new Word(Opcode.STD, 0, -1, 13), 
			new Word(Opcode.STD, 0, -1, 14), 
			new Word(Opcode.STOP, -1, -1, -1) };

	   public Word[] fibonacci10 = new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
			new Word(Opcode.LDI, 1, -1, 0), 
			new Word(Opcode.STD, 1, -1, 20),   
			new Word(Opcode.LDI, 2, -1, 1),
			new Word(Opcode.STD, 2, -1, 21),  
			new Word(Opcode.LDI, 0, -1, 22),  
			new Word(Opcode.LDI, 6, -1, 6),
			new Word(Opcode.LDI, 7, -1, 31),  
			new Word(Opcode.LDI, 3, -1, 0), 
			new Word(Opcode.ADD, 3, 1, -1),
			new Word(Opcode.LDI, 1, -1, 0), 
			new Word(Opcode.ADD, 1, 2, -1), 
			new Word(Opcode.ADD, 2, 3, -1),
			new Word(Opcode.STX, 0, 2, -1), 
			new Word(Opcode.ADDI, 0, -1, 1), 
			new Word(Opcode.SUB, 7, 0, -1),
			new Word(Opcode.JMPIG, 6, 7, -1), 
			new Word(Opcode.STOP, -1, -1, -1), 
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),   // POS 20
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1) }; // ate aqui - serie de fibonacci ficara armazenada
		
       public Word[] fatorialTRAP = new Word[] {
		   new Word(Opcode.LDI, 0, -1, 7),// numero para colocar na memoria
		   new Word(Opcode.STD, 0, -1, 50),
		   new Word(Opcode.LDD, 0, -1, 50),
		   new Word(Opcode.LDI, 1, -1, -1),
		   new Word(Opcode.LDI, 2, -1, 13),// SALVAR POS STOP
           new Word(Opcode.JMPIL, 2, 0, -1),// caso negativo pula pro STD
           new Word(Opcode.LDI, 1, -1, 1),
           new Word(Opcode.LDI, 6, -1, 1),
           new Word(Opcode.LDI, 7, -1, 13),
           new Word(Opcode.JMPIE, 7, 0, 0), //POS 9 pula pra STD (Stop-1)
           new Word(Opcode.MULT, 1, 0, -1),
           new Word(Opcode.SUB, 0, 6, -1),
           new Word(Opcode.JMP, -1, -1, 9),// pula para o JMPIE
           new Word(Opcode.STD, 1, -1, 18),
           new Word(Opcode.LDI, 8, -1, 2),// escrita
           new Word(Opcode.LDI, 9, -1, 18),//endereco com valor a escrever
           new Word(Opcode.TRAP, -1, -1, -1),
           new Word(Opcode.STOP, -1, -1, -1), // POS 17
           new Word(Opcode.DATA, -1, -1, -1)  };//POS 18	
		   
	       public Word[] fibonacciTRAP = new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
			new Word(Opcode.LDI, 8, -1, 1),// leitura
			new Word(Opcode.LDI, 9, -1, 100),//endereco a guardar
			new Word(Opcode.TRAP, -1, -1, -1),
			new Word(Opcode.LDD, 7, -1, 100),// numero do tamanho do fib
			new Word(Opcode.LDI, 3, -1, 0),
			new Word(Opcode.ADD, 3, 7, -1),
			new Word(Opcode.LDI, 4, -1, 36),//posicao para qual ira pular (stop) *
			new Word(Opcode.LDI, 1, -1, -1),// caso negativo
			new Word(Opcode.STD, 1, -1, 41),
			new Word(Opcode.JMPIL, 4, 7, -1),//pula pra stop caso negativo *
			new Word(Opcode.JMPIE, 4, 7, -1),//pula pra stop caso 0
			new Word(Opcode.ADDI, 7, -1, 41),// fibonacci + posição do stop
			new Word(Opcode.LDI, 1, -1, 0),
			new Word(Opcode.STD, 1, -1, 41),    // 25 posicao de memoria onde inicia a serie de fibonacci gerada
			new Word(Opcode.SUBI, 3, -1, 1),// se 1 pula pro stop
			new Word(Opcode.JMPIE, 4, 3, -1),
			new Word(Opcode.ADDI, 3, -1, 1),
			new Word(Opcode.LDI, 2, -1, 1),
			new Word(Opcode.STD, 2, -1, 42),
			new Word(Opcode.SUBI, 3, -1, 2),// se 2 pula pro stop
			new Word(Opcode.JMPIE, 4, 3, -1),
			new Word(Opcode.LDI, 0, -1, 43),
			new Word(Opcode.LDI, 6, -1, 25),// salva posição de retorno do loop
			new Word(Opcode.LDI, 5, -1, 0),//salva tamanho
			new Word(Opcode.ADD, 5, 7, -1),
			new Word(Opcode.LDI, 7, -1, 0),//zera (inicio do loop)
			new Word(Opcode.ADD, 7, 5, -1),//recarrega tamanho
			new Word(Opcode.LDI, 3, -1, 0),
			new Word(Opcode.ADD, 3, 1, -1),
			new Word(Opcode.LDI, 1, -1, 0),
			new Word(Opcode.ADD, 1, 2, -1),
			new Word(Opcode.ADD, 2, 3, -1),
			new Word(Opcode.STX, 0, 2, -1),
			new Word(Opcode.ADDI, 0, -1, 1),
			new Word(Opcode.SUB, 7, 0, -1),
			new Word(Opcode.JMPIG, 6, 7, -1),//volta para o inicio do loop
			new Word(Opcode.STOP, -1, -1, -1),   // POS 36
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),   // POS 41
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1)
	};

	public Word[] PB = new Word[] {
		//dado um inteiro em alguma posição de memória,
		// se for negativo armazena -1 na saída; se for positivo responde o fatorial do número na saída
		new Word(Opcode.LDI, 0, -1, 7),// numero para colocar na memoria
		new Word(Opcode.STD, 0, -1, 50),
		new Word(Opcode.LDD, 0, -1, 50),
		new Word(Opcode.LDI, 1, -1, -1),
		new Word(Opcode.LDI, 2, -1, 13),// SALVAR POS STOP
		new Word(Opcode.JMPIL, 2, 0, -1),// caso negativo pula pro STD
		new Word(Opcode.LDI, 1, -1, 1),
		new Word(Opcode.LDI, 6, -1, 1),
		new Word(Opcode.LDI, 7, -1, 13),
		new Word(Opcode.JMPIE, 7, 0, 0), //POS 9 pula pra STD (Stop-1)
		new Word(Opcode.MULT, 1, 0, -1),
		new Word(Opcode.SUB, 0, 6, -1),
		new Word(Opcode.JMP, -1, -1, 9),// pula para o JMPIE
		new Word(Opcode.STD, 1, -1, 15),
		new Word(Opcode.STOP, -1, -1, -1), // POS 14
		new Word(Opcode.DATA, -1, -1, -1)}; //POS 15

public Word[] PC = new Word[] {
		//Para um N definido (10 por exemplo)
		//o programa ordena um vetor de N números em alguma posição de memória;
		//ordena usando bubble sort
		//loop ate que não swap nada
		//passando pelos N valores
		//faz swap de vizinhos se da esquerda maior que da direita
		new Word(Opcode.LDI, 7, -1, 5),// TAMANHO DO BUBBLE SORT (N)
		new Word(Opcode.LDI, 6, -1, 5),//aux N
		new Word(Opcode.LDI, 5, -1, 46),//LOCAL DA MEMORIA
		new Word(Opcode.LDI, 4, -1, 47),//aux local memoria
		new Word(Opcode.LDI, 0, -1, 4),//colocando valores na memoria
		new Word(Opcode.STD, 0, -1, 46),
		new Word(Opcode.LDI, 0, -1, 3),
		new Word(Opcode.STD, 0, -1, 47),
		new Word(Opcode.LDI, 0, -1, 5),
		new Word(Opcode.STD, 0, -1, 48),
		new Word(Opcode.LDI, 0, -1, 1),
		new Word(Opcode.STD, 0, -1, 49),
		new Word(Opcode.LDI, 0, -1, 2),
		new Word(Opcode.STD, 0, -1, 50),//colocando valores na memoria até aqui - POS 13
		new Word(Opcode.LDI, 3, -1, 25),// Posicao para pulo CHAVE 1
		new Word(Opcode.STD, 3, -1, 99),
		new Word(Opcode.LDI, 3, -1, 22),// Posicao para pulo CHAVE 2
		new Word(Opcode.STD, 3, -1, 98),
		new Word(Opcode.LDI, 3, -1, 38),// Posicao para pulo CHAVE 3
		new Word(Opcode.STD, 3, -1, 97),
		new Word(Opcode.LDI, 3, -1, 25),// Posicao para pulo CHAVE 4 (não usada)
		new Word(Opcode.STD, 3, -1, 96),
		new Word(Opcode.LDI, 6, -1, 0),//r6 = r7 - 1 POS 22
		new Word(Opcode.ADD, 6, 7, -1),
		new Word(Opcode.SUBI, 6, -1, 1),//ate aqui
		new Word(Opcode.JMPIEM, -1, 6, 97),//CHAVE 3 para pular quando r7 for 1 e r6 0 para interomper o loop de vez do programa
		new Word(Opcode.LDX, 0, 5, -1),//r0 e r1 pegando valores das posições da memoria POS 26
		new Word(Opcode.LDX, 1, 4, -1),
		new Word(Opcode.LDI, 2, -1, 0),
		new Word(Opcode.ADD, 2, 0, -1),
		new Word(Opcode.SUB, 2, 1, -1),
		new Word(Opcode.ADDI, 4, -1, 1),
		new Word(Opcode.SUBI, 6, -1, 1),
		new Word(Opcode.JMPILM, -1, 2, 99),//LOOP chave 1 caso neg procura prox
		new Word(Opcode.STX, 5, 1, -1),
		new Word(Opcode.SUBI, 4, -1, 1),
		new Word(Opcode.STX, 4, 0, -1),
		new Word(Opcode.ADDI, 4, -1, 1),
		new Word(Opcode.JMPIGM, -1, 6, 99),//LOOP chave 1 POS 38
		new Word(Opcode.ADDI, 5, -1, 1),
		new Word(Opcode.SUBI, 7, -1, 1),
		new Word(Opcode.LDI, 4, -1, 0),//r4 = r5 + 1 POS 41
		new Word(Opcode.ADD, 4, 5, -1),
		new Word(Opcode.ADDI, 4, -1, 1),//ate aqui
		new Word(Opcode.JMPIGM, -1, 7, 98),//LOOP chave 2
		new Word(Opcode.STOP, -1, -1, -1), // POS 45
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1)};
   }
}
