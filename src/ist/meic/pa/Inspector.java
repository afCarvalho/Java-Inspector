package ist.meic.pa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.util.ArrayList;

/*TODO getdeclaredmethod ou getmethod?
 * Adaptar o codigo para reconhecer objectos do tipo array
 * Adaptar o codigo para imprimir objectos do tipo array
 * Terminar a classe graph para eliminar caminhos antigos
 * Acabar command c, falta o caso em que existem varios
 * matches para o mesmo metodo
 * Verificar a sequencia dos metodos no typematcher
 * Ultimo ponto extra
 * Perceber o porque de os modificadores necessarios nao
 * serem sempre no mesmo numero
 *  
 *  
 */

public class Inspector {

	private InfoPrinter infoPrinter;
	private TypeMatcher matcher;
	private HistoryGraph historyGraph;
	private SavedObjects savedObjects;
	private Object myObject;

	public Inspector() {
		infoPrinter = new InfoPrinter();
		matcher = new TypeMatcher();
		historyGraph = new HistoryGraph();
		savedObjects = new SavedObjects();
		myObject = null;
	}

	public void inspect(Object object) {
		myObject = object;
		infoPrinter.printInspectionInfo(object);
		historyGraph.addToHistory(object);
		readEvalPrint();
	}

	public void readEvalPrint() {
		BufferedReader buffer = new BufferedReader(new InputStreamReader(
				System.in));

		while (true) {

			System.err.print("> ");

			try {
				String arguments[] = buffer.readLine().split(" ");

				if (arguments[0].equals("q")) {
					return;
				} else if (arguments[0].equals("i")) {
					iCommand(arguments[1]);
				} else if (arguments[0].equals("m")) {
					mCommand(arguments[1], arguments[2]);
				} else if (arguments[0].equals("c")) {
					cCommand(arguments);
				} else if (arguments[0].equals("n")) {
					nCommand();
				} else if (arguments[0].equals("p")) {
					pCommand();
				} else if (arguments[0].equals("s")) {
					sCommand(arguments[1]);
				} else if (arguments[0].equals("g")) {

				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	public void iCommand(String arg) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {

		Field field = myObject.getClass().getDeclaredField(arg);

		if (Modifier.isPrivate(field.getModifiers())
				|| Modifier.isProtected(field.getModifiers()))
			field.setAccessible(true);

		myObject = field.get(myObject);
		historyGraph.addToHistory(myObject);
		infoPrinter.printInspectionInfo(myObject);

	}

	public void mCommand(String arg1, String arg2)
			throws IllegalArgumentException, IllegalAccessException,
			SecurityException, NoSuchFieldException {

		Field field = myObject.getClass().getDeclaredField(arg1);

		if (Modifier.isPrivate(field.getModifiers())
				|| Modifier.isProtected(field.getModifiers()))
			field.setAccessible(true);

		String fieldType = field.getType().toString();

		if (fieldType.equals("int"))
			field.set(myObject, matcher.IntegerMatch(arg2));
		else if (fieldType.equals("float"))
			field.set(myObject, matcher.FloatMatch(arg2));
		else if (fieldType.equals("double"))
			field.set(myObject, matcher.DoubleMatch(arg2));
		else if (fieldType.equals("long"))
			field.set(myObject, matcher.LongMatch(arg2));
		else if (fieldType.equals("byte"))
			field.set(myObject, matcher.ByteMatch(arg2));
		else if (fieldType.equals("short"))
			field.set(myObject, matcher.ShortMatch(arg2));
		else if (fieldType.equals("boolean"))
			field.set(myObject, matcher.BooleanMatch(arg2));
		else
			field.set(myObject, arg2);

		infoPrinter.printInspectionInfo(myObject);

	}

	public void cCommand(String args[]) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {

		Object result;
		ArrayList<Method> methods = new ArrayList<Method>();
		Method selectedMethod = null;

		for (Method m : myObject.getClass().getMethods()) {
			if (m.getName().equals(args[1])) {
				methods.add(m);
			}

			if (methods.size() > 1) {
				// to be continued...
				// pensar se n�o vale a pena ter s� uma vari�vel metodo
				// comparar e perceber se vale a pena substituir
			} else
				selectedMethod = methods.get(0);

			if (args.length - 2 == 0) {
				result = selectedMethod.invoke(myObject, null);
			} else {
				Object[] methodArgs = new Object[args.length - 2];

				for (int i = 0; i < args.length - 2; i++) {
					if (args[i + 2].startsWith("#")) {
						methodArgs[i] = savedObjects.getObject(args[i + 2]
								.substring(1));
					} else {
						methodArgs[i] = matcher.getBestMatch(args[i + 2]);
					}
				}

				result = selectedMethod.invoke(myObject, methodArgs);
			}

			myObject = result;
			historyGraph.addToHistory(myObject);
			infoPrinter.printInspectionInfo(myObject);

		}

	}

	public void nCommand() {
		myObject = historyGraph.getNext();
		infoPrinter.printInspectionInfo(myObject);
	}

	public void pCommand() {
		myObject = historyGraph.getPrevious();
		infoPrinter.printInspectionInfo(myObject);
	}

	public void sCommand(String arg) {
		savedObjects.saveObject(arg, myObject);
	}

	public void gCommand(String arg) {
		myObject = savedObjects.getObject(arg);
		infoPrinter.printInspectionInfo(myObject);
	}

}