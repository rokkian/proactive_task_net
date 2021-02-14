package proactive_arch;

import java.util.Map;

/* Questo è il thread che deve cercare di bloccare un task e eseguirlo o morire
 * avendoci provato
 *  
 * */

public class TaskThread extends Thread {
	String TASK_NAME;
	String lockTaskName;
	String IdAgente;
	RedisAPI redisAPI;
	Map<String, String> taskMap;
	double result, a, b;
	boolean terminate = false;
	long MAX_TASK_DURATION;
	
	public TaskThread(RedisAPI redisAPI,String IdAgente, long MAX_TASK_DURATION, String taskName) {
		super();
		this.TASK_NAME = taskName;
		this.redisAPI = redisAPI;
		this.IdAgente =  IdAgente;
		this.lockTaskName = "lock_"+taskName;
		this.MAX_TASK_DURATION = MAX_TASK_DURATION*1000;
	}
	
	private void executeTask() {
		try {
			String taskType = taskMap.get("task_type");
			this.a = Double.parseDouble(taskMap.get("a"));
			this.b = Double.parseDouble(taskMap.get("b"));
			if (taskType.equalsIgnoreCase("task+")) {
				this.result = this.a + this.b;
			} else if (taskType.equalsIgnoreCase("task-")) {
				result = a-b;
			} else if (taskType.equalsIgnoreCase("task*")) {
				result = a*b;
			} else if (taskType.equalsIgnoreCase("task/")) {
				result = a/b;
			}
			
			//aggiungo tempo di svolgimento del task fino a 10 secondi
			Thread.sleep((long)(Math.random()*this.MAX_TASK_DURATION)); 		//il task può durare fino a 10 secondi
			
		} catch (Exception e) {
			System.err.println("Task Failed!!");
			e.printStackTrace();
			this.terminate = true;
		}
	}
	
	@Override
	public void run() {
		
		long res = redisAPI.setLock(lockTaskName, true);	//converrebbe che fosse temporale per evitare i fail dei worker
		
		
		if (res==1) {	//lock acquisito
			System.out.println("Acquisito lock per "+TASK_NAME+" da agente ");

			this.taskMap = redisAPI.getHash(TASK_NAME);
			executeTask();
			if (terminate) { System.err.println("Task "+TASK_NAME+" failed"); }
			else {

				redisAPI.outputTransaction(TASK_NAME, this.result);
				System.out.println("Eseguito "+TASK_NAME+" ; svolti "+redisAPI.agentCountTasks()+" task da agente "+IdAgente);
				
			}
		}
		else if (res==0) {	//lock non acquisito
			System.out.println("Lock non acquisito per task: "+TASK_NAME);
		}	
		else {
			System.err.println("Errore grave sui lock");
		}
		
	}
}