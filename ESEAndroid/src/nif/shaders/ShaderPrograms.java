package nif.shaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.media.j3d.Shader;

import nif.j3d.NiToJ3dData;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiGeometry;
import nif.niobject.NiObject;
import nif.niobject.NiTexturingProperty;
import nif.niobject.NiTriBasedGeom;
import nif.niobject.NiTriBasedGeomData;
import nif.niobject.NiVertexColorProperty;
import nif.niobject.bs.BSEffectShaderProperty;
import nif.niobject.bs.BSLightingShaderProperty;
import nif.niobject.bs.BSMeshLODTriShape;
import nif.niobject.bs.BSShaderPPLightingProperty;
import nif.niobject.bs.BSSubIndexTriShape;
import nif.niobject.bs.BSTriShape;

public class ShaderPrograms
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
					shader.load(new File(dir, name));
					allFileShaders.put(name, shader);
				}
			}

			for (String name : dir.list())
			{
				if (name.endsWith(".frag"))
				{
					FileShader shader = new FileShader(name, Shader.SHADER_TYPE_FRAGMENT);
					shader.load(new File(dir, name));
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
						program.load(new File(dir, name), allFileShaders);
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
		private SourceCodeShader2 sourceCodeShader;
		private String name;
		private boolean status = false;

		private int type;

		public FileShader(String n, int t)
		{
			name = n;
			type = t;
		}

		boolean load(File file)
		{
			try
			{

				BufferedReader fr = new BufferedReader(new FileReader(file));

				String shaderCode = "";
				String line = fr.readLine();
				while (line != null)
				{
					shaderCode += line + "\n";
					line = fr.readLine();
				}

				fr.close();

				sourceCodeShader = new SourceCodeShader2(Shader.SHADING_LANGUAGE_GLSL, type, shaderCode);
				sourceCodeShader.name = file.getName();
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
		HashMap<String, SourceCodeShader2> shaders = new HashMap<String, SourceCodeShader2>();
		private String name;

		private boolean status = false;

		ConditionGroup conditions = new ConditionGroup();
		private HashMap<Integer, String> texcoords = new HashMap<Integer, String>();

		public Program(String name2)
		{
			this.name = name2;
		}

		boolean load(File file, HashMap<String, FileShader> allShaders) throws Exception
		{
			BufferedReader bfr = null;
			try
			{
				bfr = new BufferedReader(new FileReader(file));

				ArrayList<ConditionGroup> chkgrps = new ArrayList<ConditionGroup>();
				chkgrps.add(conditions);

				String line = bfr.readLine();

				while (line != null)
				{
					line = line.trim().toLowerCase();
					if (line.startsWith("shaders"))
					{
						String[] list = line.split(" ");

						//skip the word shaders
						for (int i = 1; i < list.length; i++)
						{
							String s = list[i];
							FileShader shader = allShaders.get(s);

							if (shader != null)
							{
								if (shader.status)
									shaders.put(shader.name, shader.sourceCodeShader);
								else
									throw new Exception(file + " program depends on shader " + s + " which was not compiled successful");
							}
							else
							{
								throw new Exception(file + " program depends on shader " + s + " which is not found");
							}
						}

					}
					else if (line.startsWith("checkgroup"))
					{
						String[] list = line.split(" ");

						if (list[1].equals("begin"))
						{
							ConditionGroup group = new ConditionGroup(list.length >= 3 && list[2].equals("or"));
							chkgrps.get(chkgrps.size() - 1).addCondition(group);
							chkgrps.add(group);
						}
						else if (list[1].equals("end"))
						{
							if (chkgrps.size() > 1)
								chkgrps.remove(chkgrps.size() - 1);
							else
								throw new Exception("mismatching checkgroup end tag in " + file);
						}
						else
						{
							throw new Exception("expected begin or end after checkgroup in " + file);
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

						int unit = new Integer(list[0]);
						String id = list[1].toLowerCase();

						if (id.length() == 0)
							throw new Exception("malformed texcoord tag in " + file);

						if (!id.equals("tangents") && !id.equals("bitangents") && TexturingPropertygetId(id) < 0)
							throw new Exception("texcoord tag refers to unknown texture id '" + id + "' in " + file);

						if (texcoords.containsKey(unit))
							throw new Exception("texture unit " + unit + " is assigned twiced in " + file);

						texcoords.put(unit, id);
					}

					line = bfr.readLine();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (bfr != null)
					bfr.close();
			}

			//Quick test to ensure texcoords are fixed, recall sk_msn has the last 2 commented out
			if (!texcoords.get(0).equals("base") || //
					(texcoords.size() > 1 && !texcoords.get(1).equals("tangents")) || //
					(texcoords.size() > 2 && !texcoords.get(2).equals("bitangents")))
			{
				System.err.println("texcords not loaded as expected in file " + file);
			}

			status = true;
			return true;
		}

		public boolean isStatusOk()
		{
			return status;
		}

		public String getName()
		{
			return name;
		}
	}

	static interface Condition
	{
		boolean eval(NiGeometry niGeometry, NiToJ3dData niToJ3dData, PropertyList props);
	}

	static class ConditionGroup implements Condition
	{
		private ArrayList<Condition> conditions = new ArrayList<Condition>();
		private boolean _or = false;

		public ConditionGroup(boolean or)
		{
			this._or = or;
		}

		public ConditionGroup()
		{

		}

		public boolean eval(NiGeometry niGeometry, NiToJ3dData niToJ3dData, PropertyList props)
		{
			if (conditions.isEmpty())
				return true;

			if (isOrGroup())
			{
				for (Condition cond : conditions)
				{
					if (cond.eval(niGeometry, niToJ3dData, props))
						return true;
				}
				return false;
			}
			else
			{
				for (Condition cond : conditions)
				{
					if (!cond.eval(niGeometry, niToJ3dData, props))
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

		private String left, right;
		private Type comp;
		private boolean invert;

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

			//Note NONE type means test for -1 cannot be used
			if (left == null)
			{
				left = line;
				comp = Type.NONE;
			}

		}

		/*
		
		check HEADER/Version >= 0x14020007
		check HEADER/User Version >= 12  >= 11
		check HEADER/User Version 2 >= 130
		
		check BSLightingShaderProperty
		#check BSLightingShaderProperty/Skyrim Shader Type != 1
		#check BSLightingShaderProperty/Skyrim Shader Type != 16		
		check BSLightingShaderProperty/Skyrim Shader Type == 3 == 11  == 2
		check NiTriBasedGeomData/Has Normals == 1  == 0
		
		check NiAVObject/Vertex Flag 1 >= 3
		check BSTriShape
		check BSSubIndexTriShape
		check BSMeshLODTriShape		
		
		#check not NiAlphaProperty
		check BSEffectShaderProperty
		
		#check NiTriBasedGeomData/Has Vertex Colors == 1
		
		check NiTexturingProperty/Apply Mode == 4
		check NiTriBasedGeom/Has Shader == 0
		
		check NiTriBasedGeomData/Has Vertex Colors == 0
		checkgroup begin
		check NiVertexColorProperty/Vertex Mode == 0
		check NiVertexColorProperty/Lighting Mode == 1
		checkgroup end
		
		check NiTriBasedGeomData/Has Vertex Colors != 0
		check NiVertexColorProperty/Vertex Mode == 1
		check NiVertexColorProperty/Lighting Mode == 1
		
		check not NiVertexColorProperty
		
		check BSShaderPPLightingProperty
		
		// 	check BSEffectShaderProperty
		// 	check NiTriBasedGeomData/Has Vertex Colors == 1
		
		
		HEADER/Version
		HEADER/User Version
		HEADER/User Version 2
		
		BSLightingShaderProperty
		BSLightingShaderProperty/Skyrim Shader Type
		
		NiTriBasedGeomData/Has Normals
		NiTriBasedGeomData/Has Vertex Colors
		
		NiAVObject/Vertex Flag 1
		
		BSTriShape
		BSSubIndexTriShape
		BSMeshLODTriShape		
		
		NiAlphaProperty
		BSEffectShaderProperty
		
		NiTexturingProperty/Apply Mode
		
		NiTriBasedGeom/Has Shader
		
		NiVertexColorProperty
		NiVertexColorProperty/Vertex Mode
		NiVertexColorProperty/Lighting Mode
				
		BSShaderPPLightingProperty
				
		BSEffectShaderProperty	
		
		
		 */
		public boolean eval(NiGeometry niGeometry, NiToJ3dData niToJ3dData, PropertyList props)
		{

			if (left.equalsIgnoreCase("HEADER/Version"))
			{
				return compare(niGeometry.nVer.LOAD_VER, Integer.decode(right)) ^ invert;
			}
			else if (left.equalsIgnoreCase("HEADER/User Version"))
			{
				return compare(niGeometry.nVer.LOAD_USER_VER, Integer.parseInt(right)) ^ invert;
			}
			else if (left.equalsIgnoreCase("User Version 2"))
			{
				return compare(niGeometry.nVer.LOAD_USER_VER2, Integer.parseInt(right)) ^ invert;
			}
			else if (left.equalsIgnoreCase("NiAVObject/Vertex Flag 1"))
			{
				//Possible mistype only BSTriShape has this attribute (I've called it vertexType) FO4 file
				if (niGeometry instanceof BSTriShape)
				{
					BSTriShape bsTriShape = (BSTriShape) niGeometry;
					return compare(bsTriShape.vertexType, Integer.parseInt(right)) ^ invert;
				}
				else
				{
					return invert;
				}
			}
			else if (left.equalsIgnoreCase("NiTriBasedGeom/Has Shader"))
			{
				if (niGeometry instanceof NiTriBasedGeom)
				{
					NiTriBasedGeom niTriBasedGeom = (NiTriBasedGeom) niGeometry;
					return compare(niTriBasedGeom.hasShader ? 1 : 0, Integer.parseInt(right)) ^ invert;
				}
				else
				{
					return invert;
				}
			}
			else if (left.equalsIgnoreCase("NiTriBasedGeomData/Has Normals"))
			{
				NiTriBasedGeomData ntbgd = (NiTriBasedGeomData) niToJ3dData.get(niGeometry.data);
				if (ntbgd == null)
					return invert;
				else
					return compare(ntbgd.hasNormals ? 1 : 0, Integer.parseInt(right)) ^ invert;
			}
			else if (left.equalsIgnoreCase("NiTriBasedGeomData/Has Vertex Colors"))
			{
				NiObject niObject = (NiObject) niToJ3dData.get(niGeometry.data);
				if (niObject == null || !(niObject instanceof NiTriBasedGeomData))
					return invert;
				else
					return compare(((NiTriBasedGeomData) niObject).hasVertexColors ? 1 : 0, Integer.parseInt(right)) ^ invert;
			}
			else if (left.equalsIgnoreCase("BSTriShape"))
			{
				return (niGeometry instanceof BSTriShape) ^ invert;
			}
			else if (left.equalsIgnoreCase("BSSubIndexTriShape"))
			{
				return (niGeometry instanceof BSSubIndexTriShape) ^ invert;
			}
			else if (left.equalsIgnoreCase("BSMeshLODTriShape"))
			{
				return (niGeometry instanceof BSMeshLODTriShape) ^ invert;
			}
			else if (left.equalsIgnoreCase("NiAlphaProperty"))
			{
				return (props.get(NiAlphaProperty.class) != null) ^ invert;
			}
			else if (left.equalsIgnoreCase("BSEffectShaderProperty"))
			{
				return (props.get(BSEffectShaderProperty.class) != null) ^ invert;
			}
			else if (left.equalsIgnoreCase("BSLightingShaderProperty"))
			{
				return (props.getBSLightingShaderProperty() != null) ^ invert;
			}
			else if (left.equalsIgnoreCase("BSLightingShaderProperty/Skyrim Shader Type"))
			{
				BSLightingShaderProperty bslsp = props.getBSLightingShaderProperty();
				if (bslsp == null)
					return invert;
				else
					return compare(bslsp.SkyrimShaderType.type, Integer.parseInt(right)) ^ invert;
			}
			else if (left.equalsIgnoreCase("NiTexturingProperty/Apply Mode"))
			{
				NiTexturingProperty p = (NiTexturingProperty) props.get(NiTexturingProperty.class);
				if (p == null)
					return invert;
				else
					return compare(p.applyMode.applyMode, Integer.parseInt(right)) ^ invert;
			}
			else if (left.equalsIgnoreCase("NiVertexColorProperty"))
			{
				return (props.get(NiVertexColorProperty.class) != null) ^ invert;
			}
			else if (left.equalsIgnoreCase("NiVertexColorProperty/Vertex Mode"))
			{
				NiVertexColorProperty p = (NiVertexColorProperty) props.get(NiVertexColorProperty.class);
				if (p == null)
					return invert;
				else
					return compare(p.vertexMode.mode, Integer.parseInt(right)) ^ invert;
			}
			else if (left.equalsIgnoreCase("NiVertexColorProperty/Lighting Mode"))
			{
				NiVertexColorProperty p = (NiVertexColorProperty) props.get(NiVertexColorProperty.class);
				if (p == null)
					return invert;
				else
					return compare(p.lightingMode.mode, Integer.parseInt(right)) ^ invert;
			}
			else if (left.equalsIgnoreCase("BSShaderPPLightingProperty"))
			{
				return (props.get(BSShaderPPLightingProperty.class) != null) ^ invert;
			}
			else if (left.equalsIgnoreCase("BSEffectShaderProperty"))
			{
				return (props.get(BSEffectShaderProperty.class) != null) ^ invert;
			}
			new Throwable("Unknown prog condition " + left + "/" + right + " " + (invert ? "not " : ""));
			if (comp == Type.NONE)
				return !invert;

			return false;
		}

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
