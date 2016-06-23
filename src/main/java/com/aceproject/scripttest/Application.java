package com.aceproject.scripttest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import groovy.lang.GroovyClassLoader;

/**
 * Script performace test
 *
 */
public class Application {
	public static class Test {
		public int call(Map<String, Object> params) {
			int n = (int) params.get("n");
			int result = 0;

			for (int i = 0; i < 10; i++)
				result += n;

			Stats stats = (Stats) params.get("stats");
			stats.setBatHit(result);

			return result;
		}
	}

	/**
	 * native java 코드 성능 측정
	 * 
	 * @param count
	 * @return
	 */
	public long testNative(int count) {
		Test obj = new Test();

		Map<String, Object> params = Maps.newHashMap();

		Stats stats = new Stats();

		long start = System.currentTimeMillis();

		for (int i = 0; i < count; i++) {
			params.put("n", i);
			params.put("stats", stats);

			Object result = obj.call(params);

			// System.out.println(result);
			// System.out.println(stats.getBatHit());
		}

		return System.currentTimeMillis() - start;
	}

	/**
	 * rhino compiled script를 사용한 성능 측정
	 * 
	 * @param count
	 * @return
	 */
	public long testRhinoCompiledScript(int count) {
		return testJsCompiledScript("rhino", count);
	}

	/**
	 * rhino invokefunction을 사용한 성능 측정
	 * 
	 * @param count
	 * @return
	 */
	public long testRhinoInvokeFunction(int count) {
		return testJsInvokeFunction("rhino", count);
	}

	/**
	 * nashorn compiled script를 사용한 성능 측정
	 * 
	 * @param count
	 * @return
	 */
	public long testNashornCompiledScript(int count) {
		return testJsCompiledScript("nashorn", count);
	}

	/**
	 * nashorn invokefunction을 사용한 성능 측정
	 * 
	 * @param count
	 * @return
	 */
	public long testNashornInvokeFunction(int count) {
		return testJsInvokeFunction("nashorn", count);
	}

	private long testJsCompiledScript(String engineName, int count) {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName(engineName);

		String script = "var result = 0; for(var i = 0; i < 10; i++) result += n; stats.batHit = result; result;";

		try {
			CompiledScript cs = ((Compilable) engine).compile(script);

			Bindings bindings = new SimpleBindings();

			Stats stats = new Stats();

			long start = System.currentTimeMillis();

			for (int i = 0; i < count; i++) {
				bindings.put("n", i);
				bindings.put("stats", stats);

				Object result = cs.eval(bindings);

				// System.out.println(result);
				// System.out.println(stats.getBatHit());
			}

			return System.currentTimeMillis() - start;
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}

	}

	private long testJsInvokeFunction(String engineName, int count) {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName(engineName);

		String script = "function test(params) {var n = parseInt(params.get(\"n\")); var result = 0; for(var i = 0; i < 10; i++) result += n; var stats = params.get(\"stats\"); stats.batHit = result; return result;}";

		try {
			engine.eval(script);

			Map<String, Object> params = Maps.newHashMap();

			Stats stats = new Stats();

			long start = System.currentTimeMillis();

			for (int i = 0; i < count; i++) {
				params.put("n", i);
				params.put("stats", stats);

				Object result = ((Invocable) engine).invokeFunction("test", params);

				// System.out.println(result);
				// System.out.println(stats.getBatHit());
			}

			return System.currentTimeMillis() - start;
		} catch (ScriptException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * groovy compile script를 사용한 성층 측정
	 * 
	 * @param count
	 * @return
	 */
	public long testGroovyCompiledScript(int count) {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("groovy");

		String script = "int result = 0; for(int i = 0; i < 10; i++) result += n; stats.batHit = result; result;";

		try {
			CompiledScript cs = ((Compilable) engine).compile(script);

			Stats stats = new Stats();

			Bindings bindings = new SimpleBindings();

			long start = System.currentTimeMillis();

			for (int i = 0; i < count; i++) {
				bindings.put("n", i);
				bindings.put("stats", stats);

				Object result = cs.eval(bindings);

				// System.out.println(result);
				// System.out.println(stats.getBatHit());
			}

			return System.currentTimeMillis() - start;
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * groovy invokefunction을 사용한 성능 측정
	 * 
	 * @param count
	 * @return
	 */
	public long testGroovyInvokeFunction(int count) {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("groovy");

		String script = "def test(Map params) {int n = params.get(\"n\"); int result = 0; for(int i = 0; i < 10; i++) result += n; def stats = params.get(\"stats\"); stats.batHit = result; result;}";

		try {
			engine.eval(script);

			Map<String, Object> params = Maps.newHashMap();

			Stats stats = new Stats();

			long start = System.currentTimeMillis();

			for (int i = 0; i < count; i++) {
				params.put("n", i);
				params.put("stats", stats);

				Object result = ((Invocable) engine).invokeFunction("test", params);

				// System.out.println(result);
				// System.out.println(stats.getBatHit());
			}

			return System.currentTimeMillis() - start;
		} catch (ScriptException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * groovy classloader를 사용한 성능 측정
	 * 
	 * @param count
	 * @return
	 */
	public long testGroovyClassLoader(int count) {
		GroovyClassLoader gcl = new GroovyClassLoader();

		String script = "def test(Map params) {int n = params.get(\"n\"); int result = 0; for(int i = 0; i < 10; i++) result += n; def stats = params.get(\"stats\"); stats.batHit = result; result;}";

		Class clazz = gcl.parseClass(script);

		try {
			Object obj = clazz.newInstance();
			Method method = clazz.getMethod("test", Map.class);

			Map<String, Object> params = Maps.newHashMap();

			Stats stats = new Stats();

			long start = System.currentTimeMillis();

			for (int i = 0; i < count; i++) {
				params.put("n", i);
				params.put("stats", stats);

				Object result = method.invoke(obj, params);

				// System.out.println(result);
				// System.out.println(stats.getBatHit());
			}

			return System.currentTimeMillis() - start;
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * JavaCompiler를 사용한 동적 클래스 생성을 이용한 성능 측정
	 * 
	 * @param count
	 * @return
	 */
	public long testJavaCompiler(int count) {
		// reference : http://www.rgagnon.com/javadetails/java-0039.html
		// http://www.informit.com/articles/article.aspx?p=2027052&seqNum=2

		StringBuilder builder = new StringBuilder();
		builder.append("import java.util.Map;").append("\n");
		builder.append("import com.aceproject.scripttest.Stats;").append("\n");
		builder.append("public class Test2 {").append("\n");
		builder.append("	public int call(Map<String, Object> params) {").append("\n");
		builder.append("		int n = (int) params.get(\"n\");").append("\n");
		builder.append("		int result = 0;").append("\n");
		builder.append("		for(int i = 0; i < 10; i++) result += n;").append("\n");
		builder.append("		Stats stats = (Stats) params.get(\"stats\");").append("\n");
		builder.append("		stats.setBatHit(result);").append("\n");
		builder.append("		return result;").append("\n");
		builder.append("	}").append("\n");
		builder.append("}").append("\n");

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		JavaSourceFromString file = new JavaSourceFromString("Test2", builder.toString());

		List<ByteArrayJavaClass> classFileObjects = new ArrayList<>();

		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

		JavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		fileManager = new ForwardingJavaFileManager<JavaFileManager>(fileManager) {

			@Override
			public JavaFileObject getJavaFileForOutput(Location location, String className,
					javax.tools.JavaFileObject.Kind kind, FileObject sibling) throws IOException {
				if (className.startsWith("Test")) {
					ByteArrayJavaClass fileObject = new ByteArrayJavaClass(className);
					classFileObjects.add(fileObject);
					return fileObject;
				}
				return super.getJavaFileForOutput(location, className, kind, sibling);
			}
		};

		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null,
				Lists.newArrayList(file));
		task.call();

		for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics())
			System.out.println(d.getKind() + ": " + d.getMessage(null));

		Map<String, byte[]> byteCodeMap = new HashMap<>();

		for (ByteArrayJavaClass cl : classFileObjects) {
			byteCodeMap.put(cl.getName().substring(1), cl.getBytes());
		}

		try {
			ClassLoader loader = new MapClassLoader(byteCodeMap);
			Class clazz = loader.loadClass("Test2");

			Object obj = clazz.newInstance();
			Method method = clazz.getMethod("call", Map.class);

			Map<String, Object> params = Maps.newHashMap();

			Stats stats = new Stats();

			long start = System.currentTimeMillis();

			for (int i = 0; i < count; i++) {
				params.put("n", i);
				params.put("stats", stats);

				Object result = method.invoke(obj, params);

				// System.out.println(result);
				// System.out.println(stats.getBatHit());
			}

			return System.currentTimeMillis() - start;

		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
				| SecurityException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public class JavaSourceFromString extends SimpleJavaFileObject {
		/**
		 * The source code of this "file".
		 */
		final String code;

		/**
		 * Constructs a new JavaSourceFromString.
		 * 
		 * @param name
		 *            the name of the compilation unit represented by this file
		 *            object
		 * @param code
		 *            the source code for the compilation unit represented by
		 *            this file object
		 */
		JavaSourceFromString(String name, String code) {
			super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.code = code;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}
	}

	public class ByteArrayJavaClass extends SimpleJavaFileObject {
		private ByteArrayOutputStream stream;

		/**
		 * Constructs a new ByteArrayJavaClass.
		 * 
		 * @param name
		 *            the name of the class file represented by this file object
		 */
		public ByteArrayJavaClass(String name) {
			super(URI.create("bytes:///" + name), Kind.CLASS);
			stream = new ByteArrayOutputStream();
		}

		@Override
		public OutputStream openOutputStream() throws IOException {
			return stream;
		}

		public byte[] getBytes() {
			return stream.toByteArray();
		}
	}

	public class MapClassLoader extends ClassLoader {
		private Map<String, byte[]> classes;

		public MapClassLoader(Map<String, byte[]> classes) {
			this.classes = classes;
		}

		protected Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] classBytes = classes.get(name);
			if (classBytes == null)
				throw new ClassNotFoundException(name);
			Class<?> cl = defineClass(name, classBytes, 0, classBytes.length);
			if (cl == null)
				throw new ClassNotFoundException(name);
			return cl;
		}
	}

	/**
	 * java reflection을 사용한 성능 측정
	 * 
	 * @param count
	 * @return
	 */
	public long testReflection(int count) {
		try {
			Class clazz = Class.forName("com.aceproject.scripttest.Application$Test");
			Object obj = clazz.newInstance();
			Method method = clazz.getMethod("call", Map.class);

			Map<String, Object> params = Maps.newHashMap();

			Stats stats = new Stats();

			long start = System.currentTimeMillis();

			for (int i = 0; i < count; i++) {
				params.put("n", i);
				params.put("stats", stats);

				Object result = method.invoke(obj, params);

				// System.out.println(result);
				// System.out.println(stats.getBatHit());
			}

			return System.currentTimeMillis() - start;
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException
				| IllegalArgumentException | InvocationTargetException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		Application app = new Application();

		int count = 1000000;

		for (int i = 0; i < 10; i++) {
			System.out.println("===== " + (i + 1) + " =====");
			System.out.println("native : " + app.testNative(count) + " ms");
			System.out.println("rhino (compiledScript) : " + app.testRhinoCompiledScript(count) + " ms");
			System.out.println("rhino (invokeFunction) : " + app.testRhinoInvokeFunction(count) + " ms");
			System.out.println("nashron (compiledScript) : " + app.testNashornCompiledScript(count) + " ms");
			System.out.println("nashron (invokeFunction) : " + app.testNashornInvokeFunction(count) + " ms");
			System.out.println("groovy (compiledScript) : " + app.testGroovyCompiledScript(count) + " ms");
			System.out.println("groovy (invokeFunction) : " + app.testGroovyInvokeFunction(count) + " ms");
			System.out.println("groovy (classLoader) : " + app.testGroovyClassLoader(count) + " ms");
			System.out.println("javaCompiler : " + app.testJavaCompiler(count) + " ms");
			System.out.println("reflection : " + app.testReflection(count) + " ms");
			System.out.println();

		}
	}
}
