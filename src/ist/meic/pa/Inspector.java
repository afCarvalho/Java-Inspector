package ist.meic.pa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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
 */

public class Inspector {

	private HistoryGraph historyGraph;
	private SavedObjects savedObjects;
	private Object object;

	public Inspector() {
		historyGraph = new HistoryGraph();
		savedObjects = new SavedObjects();
		object = null;
	}

	public void inspect(Object object) {
		this.object = object;
		InfoPrinter.printInspectionInfo(object);
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
					buffer.close();
					return;
				} else if (arguments[0].equals("i")) {
					if (arguments.length < 3) {
						inspect(arguments[1], 0);
					} else {
						inspect(arguments[1], Integer.parseInt(arguments[2]));
					}
				} else if (arguments[0].equals("m")) {
					modify(arguments[1], arguments[2]);
				} else if (arguments[0].equals("c")) {
					call(arguments);
				} else if (arguments[0].equals("n")) {
					next();
				} else if (arguments[0].equals("p")) {
					previous();
				} else if (arguments[0].equals("s")) {
					save(arguments[1]);
				} else if (arguments[0].equals("g")) {
					get(arguments[1]);
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
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void inspect(String name, int value) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		int i = 0;

		if (value > 0) {
			while (i != value) {
				object = object.getClass().getSuperclass().newInstance();
				i++;
			}
		}

		Field field = object.getClass().getDeclaredField(name);

		if (Modifier.isPrivate(field.getModifiers())
				|| Modifier.isProtected(field.getModifiers())) {
			field.setAccessible(true);
		}

		object = field.get(object);
		historyGraph.addToHistory(object);
		InfoPrinter.printInspectionInfo(object);
	}

	private void modify(String name, String value)
			throws IllegalArgumentException, IllegalAccessException,
			SecurityException, NoSuchFieldException {

		Field field = object.getClass().getDeclaredField(name);

		if (Modifier.isPrivate(field.getModifiers())
				|| Modifier.isProtected(field.getModifiers())) {
			field.setAccessible(true);
		}

		String type = field.getType().toString();

		if (type.equals("int")) {
			field.set(object, TypeMatcher.IntegerMatch(value));
		} else if (type.equals("float")) {
			field.set(object, TypeMatcher.FloatMatch(value));
		} else if (type.equals("double")) {
			field.set(object, TypeMatcher.DoubleMatch(value));
		} else if (type.equals("long")) {
			field.set(object, TypeMatcher.LongMatch(value));
		} else if (type.equals("byte")) {
			field.set(object, TypeMatcher.ByteMatch(value));
		} else if (type.equals("short")) {
			field.set(object, TypeMatcher.ShortMatch(value));
		} else if (type.equals("boolean")) {
			field.set(object, TypeMatcher.BooleanMatch(value));
		} else {
			field.set(object, value);
		}

		InfoPrinter.printInspectionInfo(object);
	}

	private void call(String args[]) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {

		Object[] methodArgs = new Object[args.length - 2];
		Class<?> myClass;

		if (object != null) {

			for (int i = 0; i < args.length - 2; i++) {
				if (args[i + 2].startsWith("#")) {
					methodArgs[i] = savedObjects.getObject(args[i + 2].substring(1));
				} else {
					methodArgs[i] = getBestMatch(args[i + 2]);
				}
			}

			// verifica partindo da classe actual, passando depois `as
			// superclasses
			// se ha algum metodo com o mesmo nome
			myClass = object.getClass();

			while (!myClass.isInstance(Object.class)) {

				for (Method m : myClass.getMethods()) {
					if (m.getName().equals(args[1])
							&& hasCompatibleArgs(m, methodArgs)) {
						object = m.invoke(object, methodArgs);
						historyGraph.addToHistory(object);
						InfoPrinter.printInspectionInfo(object);
						return;
					}
				}
				myClass = myClass.getSuperclass();
			}
		} else {
			InfoPrinter
					.printInspectionInfo("cCommand: the object invocated does not exist");
		}
	}

	public boolean hasCompatibleArgs(Method m, Object args[]) {
		for (int i = 0; i < args.length; i++) {
			if (!m.getParameterTypes()[i].getName().equals(
					args[i].getClass().getName())) {
				return false;
			}
		}

		return m.getParameterTypes().length == args.length;
	}

	public static Object getBestMatch(String s) {
		try {
			for (Method m : TypeMatcher.class.getDeclaredMethods()) {
				try {
					return m.invoke(TypeMatcher.class, s);
				} catch (NumberFormatException e) {
					continue;
				}
			}

		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return s;
	}

	private void next() {
		object = historyGraph.getNext();
		InfoPrinter.printInspectionInfo(object);
	}

	private void previous() {
		object = historyGraph.getPrevious();
		InfoPrinter.printInspectionInfo(object);
	}

	private void save(String arg) {
		savedObjects.saveObject(arg, object);
	}

	private void get(String arg) {
		object = savedObjects.getObject(arg);
		if (object != null) {
			InfoPrinter.printInspectionInfo(object);
		}
	}
}
