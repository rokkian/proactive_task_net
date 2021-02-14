import java.util.ArrayList;
import java.util.List;

/* Questo l'ho fatto a oggetto ma potrebbe diventare Thread anche questo.
 * Crea tanti ThreadTask quanti sono i task nella lista su redis
 * 
 * */

public class DataspaceQuery implements DataspaceQueryInterface, Runnable{
	String IdAgente;
	RedisAPI redisAPI;
	List<String> taskList;
	List<TaskThread> taskThreadList;
	long MAX_TASK_DURATION;	//massima durata di un task in secondi

	public DataspaceQuery(String IdAgente, long MAX_TASK_DURATION, RedisAPI redisAPI) {
		super();
		this.IdAgente = IdAgente;
		this.redisAPI = redisAPI;
		this.MAX_TASK_DURATION = MAX_TASK_DURATION;
	}

	public void run() {
		taskThreadList = new ArrayList<TaskThread>();
		this.taskList = redisAPI.readTaskList();
		
		//Qui posso fare operazioni di filtraggio task (che cerco di svolgere o no)
		System.out.println("Ho letto "+this.taskList.size()+" task dalla lista");
		//int thread_aperti =1;
		for (String task_name : this.taskList) {
			
			taskThreadList.add(new TaskThread(this.redisAPI, this.IdAgente, this.MAX_TASK_DURATION, task_name));
			taskThreadList.get(taskThreadList.size()-1).start();
		}
		
	}
}