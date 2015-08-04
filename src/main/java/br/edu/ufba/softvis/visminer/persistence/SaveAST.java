package br.edu.ufba.softvis.visminer.persistence;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import br.edu.ufba.softvis.visminer.ast.AST;
import br.edu.ufba.softvis.visminer.ast.ClassOrInterfaceDeclaration;
import br.edu.ufba.softvis.visminer.ast.EnumConstantDeclaration;
import br.edu.ufba.softvis.visminer.ast.EnumDeclaration;
import br.edu.ufba.softvis.visminer.ast.FieldDeclaration;
import br.edu.ufba.softvis.visminer.ast.MethodDeclaration;
import br.edu.ufba.softvis.visminer.ast.PackageDeclaration;
import br.edu.ufba.softvis.visminer.ast.Project;
import br.edu.ufba.softvis.visminer.ast.TypeDeclaration;
import br.edu.ufba.softvis.visminer.constant.SoftwareUnitType;
import br.edu.ufba.softvis.visminer.model.database.FileDB;
import br.edu.ufba.softvis.visminer.model.database.RepositoryDB;
import br.edu.ufba.softvis.visminer.model.database.SoftwareUnitDB;
import br.edu.ufba.softvis.visminer.persistence.dao.SoftwareUnitDAO;
import br.edu.ufba.softvis.visminer.persistence.impl.SoftwareUnitDAOImpl;
import br.edu.ufba.softvis.visminer.utility.StringUtils;

public class SaveAST {

	private Map<String, Integer> uidMap;
	private SoftwareUnitDAO softUnitDao;
	private Project project;
	private SoftwareUnitDB projectUnit;
	private RepositoryDB repositoryDb;
	
	public SaveAST(RepositoryDB repositoryDb, EntityManager entityManager){
		
		uidMap = new HashMap<String, Integer>();
		
		softUnitDao = new SoftwareUnitDAOImpl();
		softUnitDao.setEntityManager(entityManager);
		
		this.repositoryDb = repositoryDb;
		projectUnit = softUnitDao.findByUid(repositoryDb.getUid());
		
		project = new Project();
		project.setId(projectUnit.getId());
		project.setName(projectUnit.getName());

	}
	
	public void save(FileDB fileDb, AST ast){

		SoftwareUnitDB parent = null;
		ast.setProject(project);
		
		if(ast.getDocument().getPackageDeclaration() != null){
				
			PackageDeclaration pkgDecl = ast.getDocument().getPackageDeclaration();
			String uid = generateUid(repositoryDb.getUid(), null, pkgDecl.getName());
				
			SoftwareUnitDB pkgUnit = getSofwareUnitDB(uid, pkgDecl.getName(), SoftwareUnitType.PACKAGE,
						null, repositoryDb, projectUnit);
			pkgDecl.setId(pkgUnit.getId());
			parent = pkgUnit;	
			
		}else{
			parent = projectUnit;
		}
		
		String docUid = generateUid(repositoryDb.getUid(), parent.getUid(), fileDb.getUid());
		SoftwareUnitDB docUnit = getSofwareUnitDB(docUid, ast.getDocument().getName(), SoftwareUnitType.FILE,
				fileDb, repositoryDb, parent);
		ast.getDocument().setId(docUnit.getId());
		
		if(ast.getDocument().getTypes() != null){
			for(TypeDeclaration type : ast.getDocument().getTypes()){
				
				String typeUid = generateUid(repositoryDb.getUid(), docUid, type.getName());
				
				SoftwareUnitDB typeUnit = getSofwareUnitDB(typeUid, type.getName(), type.getType(),
						fileDb, repositoryDb, docUnit);
				type.setId(typeUnit.getId());
				
				if(type.getType() == SoftwareUnitType.CLASS_OR_INTERFACE){
					processClassOrInterface( (ClassOrInterfaceDeclaration) type, fileDb, typeUnit);
				}else if(type.getType() == SoftwareUnitType.ENUM){
					processEnum( (EnumDeclaration) type, typeUnit, fileDb);
				}
				
			}
		}
			
	}
	
	private String generateUid(String repositoryUid, String parentUid, String softwareUnitName){
		String uid = repositoryUid + parentUid + softwareUnitName;
		return StringUtils.sha1(uid);
	}
	
	private void processClassOrInterface(ClassOrInterfaceDeclaration type, FileDB fileDb, SoftwareUnitDB typeUnit){
		
		if(type.getFields() != null){
			for(FieldDeclaration field : type.getFields()){
				String fieldUid = generateUid(repositoryDb.getUid(), typeUnit.getUid(), type.getName()+"."+field.getName());
				SoftwareUnitDB fieldUnit = getSofwareUnitDB(fieldUid, field.getName(), SoftwareUnitType.FIELD,
						fileDb, repositoryDb, typeUnit);
				field.setId(fieldUnit.getId());
			}
		}

		if(type.getMethods() != null){
			for(MethodDeclaration method : type.getMethods()){
				String methodUid = generateUid(repositoryDb.getUid(), typeUnit.getUid(), type.getName()+"."+method.getName());
				SoftwareUnitDB methodUnit = getSofwareUnitDB(methodUid, method.getName(), SoftwareUnitType.METHOD,
						fileDb, repositoryDb, typeUnit);
				method.setId(methodUnit.getId());
			}
		}
		
	}
	
	private void processEnum(EnumDeclaration type, SoftwareUnitDB enumUnit, FileDB fileDb){
		
		if(type.getEnumConsts() != null){
			for(EnumConstantDeclaration constDecl : type.getEnumConsts()){
				String constUid = generateUid(repositoryDb.getUid(), enumUnit.getUid(), constDecl.getName());
				SoftwareUnitDB constUnit = getSofwareUnitDB(constUid, constDecl.getName(), SoftwareUnitType.ENUM_CONST, fileDb,
						repositoryDb, enumUnit);
				constDecl.setId(constUnit.getId());
			}
		}
		
	}
	
	private SoftwareUnitDB getSofwareUnitDB(String uid, String name, SoftwareUnitType type, FileDB fileDb,
			RepositoryDB repoDb, SoftwareUnitDB parent){
		
		SoftwareUnitDB softwareUnitDB = new SoftwareUnitDB();
		
		if(uidMap.containsKey(uid)){
			softwareUnitDB.setId(uidMap.get(uid));
		}else{				
			
			softwareUnitDB.setName(name);
			softwareUnitDB.setUid(uid);
			softwareUnitDB.setType(type);
			softwareUnitDB.setFile(fileDb);
			softwareUnitDB.setRepository(repoDb);
			softwareUnitDB.setSoftwareUnit(parent);
			softUnitDao.save(softwareUnitDB);
			uidMap.put(uid, softwareUnitDB.getId());
		}

		return softwareUnitDB;
		
	}
	
}