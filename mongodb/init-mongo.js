const databaseName = process.env.MONGO_APP_DATABASE || "smart_wms_documents";
const username = process.env.MONGO_APP_USERNAME || "smart_wms";
const password = process.env.MONGO_APP_PASSWORD;

if (!password) {
  throw new Error("MONGO_APP_PASSWORD must be configured");
}

const applicationDatabase = db.getSiblingDB(databaseName);
if (!applicationDatabase.getUser(username)) {
  applicationDatabase.createUser({
    user: username,
    pwd: password,
    roles: [{ role: "readWrite", db: databaseName }]
  });
}
