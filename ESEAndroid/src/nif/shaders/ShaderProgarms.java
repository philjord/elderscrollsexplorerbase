package nif.shaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.media.j3d.GLSLShaderProgram;
import javax.media.j3d.Shader;
import javax.media.j3d.SourceCodeShader;

import nif.j3d.NiToJ3dData;
import nif.niobject.NiGeometry;

public class ShaderProgarms
{
	private static HashMap<String, FileShader> allFileShaders;
	public static HashMap<String, Program> programs;

	public static void loadShaderPrograms()
	{
		if (programs == null)
		{
			programs = new HashMap<String, Program>();
			allFileShaders = new HashMap<String, FileShader>();
			File dir = new File("shaders/nif");

			for (String name : dir.list())
			{
				if (name.endsWith(".vert"))
				{
					FileShader shader = new FileShader(name, Shader.SHADER_TYPE_VERTEX);
					shader.load(name);
					allFileShaders.put(name, shader);
				}
			}

			for (String name : dir.list())
			{
				if (name.endsWith(".frag"))
				{
					FileShader shader = new FileShader(name, Shader.SHADER_TYPE_FRAGMENT);
					shader.load(name);
					allFileShaders.put(name, shader);
				}
			}

			for (String name : dir.list())
			{
				if (name.endsWith(".prog"))
				{
					Program program = new Program(name);
					try
					{
						program.load(name, allFileShaders);
						programs.put(name, program);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}

	static class FileShader
	{
		SourceCodeShader sourceCodeShader;
		String name;
		boolean status = false;

		int type;

		public FileShader(String n, int t)
		{
			name = n;
			type = t;
		}

		boolean load(String filepath)
		{
			try
			{

				BufferedReader fr = new BufferedReader(new FileReader(filepath));

				String shaderCode = "";
				String line = fr.readLine();
				while (line != null)
				{
					shaderCode += line + "\n";
					line = fr.readLine();
				}

				fr.close();

				sourceCodeShader = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, type, shaderCode);
				status = true;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			return true;
		}
	}

	static class Program
	{
		HashMap<String, FileShader> fileShaders = new HashMap<String, FileShader>();
		String name;
		GLSLShaderProgram shaderProgram = new GLSLShaderProgram();
		boolean status = false;

		ConditionGroup conditions = new ConditionGroup();
		HashMap<Integer, String> texcoords = new HashMap<Integer, String>();

		public Program(String name2)
		{
			this.name = name2;
		}

		boolean load(String filepath, HashMap<String, FileShader> allShaders) throws Exception
		{
			BufferedReader bfr = null;
			try
			{
				bfr = new BufferedReader(new FileReader(filepath));

				ArrayList<ConditionGroup> chkgrps = new ArrayList<ConditionGroup>();
				chkgrps.add(conditions);

				String line = bfr.readLine().trim().toLowerCase();

				while (line != null)
				{

					if (line.startsWith("shaders"))
					{
						String[] list = line.split(" ");

						ArrayList<Shader> shaders = new ArrayList<Shader>();
						for (String s : list)
						{
							FileShader shader = allShaders.get(s);

							if (shader != null)
							{
								if (shader.status)
									shaders.add(shader.sourceCodeShader);
								else
									throw new Exception(
											filepath + " program depends on shader " + s + " which was not compiled successful");
							}
							else
							{
								throw new Exception(filepath + " programdepends on shader " + s + " which is not found");
							}
						}

						shaderProgram.setShaders(shaders.toArray(new Shader[] {}));

					}
					else if (line.startsWith("checkgroup"))
					{
						String[] list = line.split(" ");

						if (list[1].equals("begin"))
						{
							ConditionGroup group = new ConditionGroup(list[2].equals("or"));
							chkgrps.get(chkgrps.size() - 1).addCondition(group);
							chkgrps.add(group);
						}
						else if (list[1].equals("end"))
						{
							if (chkgrps.size() > 1)
								chkgrps.remove(chkgrps.size() - 1);
							else
								throw new Exception("mismatching checkgroup end tag");
						}
						else
						{
							throw new Exception("expected begin or end after checkgroup");
						}
					}
					else if (line.startsWith("check"))
					{
						line = line.substring("check".length()).trim();

						boolean invert = false;

						if (line.startsWith("not "))
						{
							invert = true;
							line = line.substring("not ".length()).trim();
						}

						chkgrps.get(chkgrps.size() - 1).addCondition(new ConditionSingle(line, invert));
					}
					else if (line.startsWith("texcoords"))
					{
						line = line.substring("texcoords".length()).trim();
						String[] list = line.split(" ");
						boolean ok = false;
						int unit = new Integer(list[0]);
						String id = list[1].toLowerCase();

						if (!ok || id.isEmpty())
							throw new Exception("malformed texcoord tag");

						if (id != "tangents" && id != "bitangents" && TexturingPropertygetId(id) < 0)
							throw new Exception("texcoord tag referres to unknown texture id '" + id + "'");

						if (texcoords.containsKey(unit))
							throw new Exception("texture unit " + unit + " is assigned twiced");

						texcoords.put(unit, id);
					}

					line = bfr.readLine().trim().toLowerCase();
				}
				bfr.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				bfr.close();
			}

			//Quick test to ensure texcoords are fixed
			if (!texcoords.get(0).equals("base") || !texcoords.get(1).equals("tangents") || !texcoords.get(2).equals("bitangents"))
			{
				System.err.println("texcords not loaded as expected in file " + filepath);
			}

			status = true;
			return true;
		}
	}

	/*
	 * # default shader
	
	checkgroup begin or
	# Fallout 4 and later
	checkgroup begin and
		check HEADER/Version >= 0x14020007
		check HEADER/User Version >= 12
	check HEADER/User Version 2 >= 130
	checkgroup end
	checkgroup end
	
	checkgroup begin and
	check BSLightingShaderProperty
	#check BSLightingShaderProperty/Skyrim Shader Type != 1
	#check BSLightingShaderProperty/Skyrim Shader Type != 16
	check NiAVObject/Vertex Flag 1 >= 3
	checkgroup begin or
	check BSTriShape
	check BSSubIndexTriShape
	check BSMeshLODTriShape
	checkground end
	checkgroup end
	
	texcoords 0 base
	texcoords 1 tangents
	texcoords 2 bitangents
	
	shaders fo4_default.vert fo4_default.frag
	*/

	static interface Condition
	{
		boolean eval(NiGeometry niGeometry, NiToJ3dData niToJ3dData);
	}

	static class ConditionGroup implements Condition
	{
		ArrayList<Condition> conditions;
		boolean _or = false;

		public ConditionGroup(boolean or)
		{
			this._or = or;
		}

		public ConditionGroup()
		{

		}

		public boolean eval(NiGeometry niGeometry, NiToJ3dData niToJ3dData)
		{
			if (conditions.isEmpty())
				return true;

			if (isOrGroup())
			{
				for (Condition cond : conditions)
				{
					if (cond.eval(niGeometry, niToJ3dData))
						return true;
				}
				return false;
			}
			else
			{
				for (Condition cond : conditions)
				{
					if (!cond.eval(niGeometry, niToJ3dData))
						return false;
				}
				return true;
			}
		}

		void addCondition(Condition c)
		{
			conditions.add(c);
		}

		boolean isOrGroup()
		{
			return _or;
		}
	}

	enum Type
	{
		EQ(" == "), NE(" != "), LE(" <= "), GE(" >= "), LT(" < "), GT(" > "), AND(" & "), NONE("");

		private final String sign;

		Type(String sign)
		{
			this.sign = sign;

		}

		private String sign()
		{
			return sign;
		}
	};

	static class ConditionSingle implements Condition
	{

		String left, right;
		Type comp;
		boolean invert;

		public ConditionSingle(String line, boolean neg)
		{
			invert = neg;

			int pos = -1;
			for (Type t : Type.values())
			{

				pos = line.indexOf(t.sign());

				if (pos > 0)
				{
					left = line.substring(0, pos).trim();
					right = line.substring(pos + t.sign().length()).trim();

					if (right.startsWith("\"") && right.endsWith("\""))
						right = right.substring(1, right.length() - 2);

					comp = t;
					break;
				}
			}

			if (pos == -1)
			{
				left = line;
				comp = Type.NONE;
			}
		}

		public boolean eval(NiGeometry niGeometry, NiToJ3dData niToJ3dData)
		{

			return false;

			//OK I can't pull these things back as easily as I'd like, so a fat map of
			// check string to the current J3dNiGeometry sub parts
			// and the header too!

			/*int iLeft = getIndex(niToJ3dData, left);
			
			if (iLeft != -1)
				return invert;
			
			if (comp == Type.NONE)
				return !invert;
			
			NiObject val = niToJ3dData.getNiObjects().getNiObjects()[iLeft];
			
			if (val.isString())
				return compare(val.toString(), right) ^ invert;
			else if (val.isCount())
				return compare(val.toCount(), Integer.parseInt(right)) ^ invert;
			else if (val.isFloat())
				return compare(val.toFloat(), (float) Double.parseDouble(right)) ^ invert;
			else if (val.isFileVersion())
				return compare(val.toFileVersion(), Integer.parseInt(right)) ^ invert;
			
			return false;*/
		}

		/*int getIndex(NiToJ3dData niToJ3dData, String blkid)
		{
			String childid;
		
			if (blkid.startsWith("HEADER/"))
				return nif.getIndex(nif.getHeader(), blkid);
		
			int pos = blkid.indexOf("/");
		
			if (pos > 0)
			{
				childid = blkid.substring(blkid.length() - pos - 1);
				blkid = blkid.substring(0, pos);
			}
		
			for (QModelIndex iBlock : iBlocks)
			{
				if (nif.inherits(iBlock, blkid))
				{
					if (childid.isEmpty())
						return iBlock;
		
					return nif.getIndex(iBlock, childid);
				}
			}
			return -1;
		}
		*/

		/*	<T> boolean compare(T a, T b)
			{
				switch (comp)
				{
				case EQ:
					return a == b;
				case NE:
					return a != b;
				case LE:
					return a <= b;
				case GE:
					return a >= b;
				case LT:
					return a < b;
				case GT:
					return a > b;
				case AND:
					return a & b;
				default:
					return true;
				}
			}*/
		boolean compare(int a, int b)
		{
			switch (comp)
			{
			case EQ:
				return a == b;
			case NE:
				return a != b;
			case LE:
				return a <= b;
			case GE:
				return a >= b;
			case LT:
				return a < b;
			case GT:
				return a > b;
			default:
				return true;
			}
		}

		boolean compare(float a, float b)
		{
			switch (comp)
			{
			case EQ:
				return a == b;
			case NE:
				return a != b;
			case LE:
				return a <= b;
			case GE:
				return a >= b;
			case LT:
				return a < b;
			case GT:
				return a > b;
			default:
				return true;
			}
		}

		boolean compare(String a, String b)
		{
			switch (comp)
			{
			case EQ:
				return a == b;
			case NE:
				return a != b;
			default:
				return false;
			}
		}

	}

	enum tpnames
	{
		base, dark, detail, gloss, glow, bumpmap, decal0, decal1, decal2, decal3
	};

	static int TexturingPropertygetId(String texname)
	{
		return tpnames.valueOf(texname).ordinal();
	}

	enum bsslpnames
	{
		base, dark, detail, gloss, glow, bumpmap, decal0, decal1
	};

	static int BSShaderLightingPropertygetId(String id)
	{
		return bsslpnames.valueOf(id).ordinal();
	}
}
