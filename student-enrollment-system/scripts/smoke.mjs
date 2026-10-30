import assert from 'node:assert/strict';

const base = process.env.SMOKE_API_URL || 'http://127.0.0.1:8080/api';
const username = process.env.SMOKE_ADMIN_USERNAME;
const password = process.env.SMOKE_ADMIN_PASSWORD;
if (!username || !password) throw new Error('Set SMOKE_ADMIN_USERNAME and SMOKE_ADMIN_PASSWORD for a disposable demo environment');

async function request(method, path, body, token, expected = 200, origin = base) {
  const response = await fetch(origin + path, {
    method, headers: { ...(body ? {'Content-Type':'application/json'} : {}), ...(token ? {Authorization:'Bearer '+token} : {}) },
    ...(body ? {body: JSON.stringify(body)} : {}), signal: AbortSignal.timeout(20000)
  });
  const text = await response.text();
  assert.equal(response.status, expected, method+' '+path+': '+text);
  return text ? JSON.parse(text) : null;
}

const registry = await fetch(process.env.SMOKE_EUREKA_URL || 'http://127.0.0.1:8761/eureka/apps', {headers:{Accept:'application/json'}});
assert.equal(registry.status, 200);
const applications = (await registry.json()).applications.application;
for (const service of ['STUDENT-SERVICE','COURSE-SERVICE','ENROLLMENT-SERVICE','API-GATEWAY']) {
  assert(applications.some(a => a.name === service && a.instance.some(i => i.status === 'UP')), 'Missing Eureka registration: '+service);
}
console.log('PASS Eureka registrations');

const admin = (await request('POST','/auth/login',{username,password})).accessToken;
const suffix = String(Date.now()).slice(-7);
const cnie = 'SM'+suffix;
const student = await request('POST','/students',{cnie,firstName:'Smoke',lastName:'Student',email:suffix+'@example.test'},admin,201);
const other = await request('POST','/students',{cnie:'OT'+suffix,firstName:'Other',lastName:'Student',email:'other'+suffix+'@example.test'},admin,201);
const course = await request('POST','/courses',{title:'Smoke '+suffix,description:'Disposable integration fixture',credits:4},admin,201);
await request('PUT','/students/'+student.id,{...student,firstName:'Updated'},admin);
await request('PUT','/courses/'+course.id,{...course,description:'Updated fixture'},admin);
const studentPassword = 'Disposable-test-'+suffix;
await request('POST','/auth/accounts',{username:'smoke-'+suffix,password:studentPassword,studentId:student.id},admin,201);
const login = await request('POST','/auth/login',{username:'smoke-'+suffix,password:studentPassword});
assert.equal(login.role,'STUDENT');
const token = login.accessToken;
await request('GET','/courses',null,token);
await request('GET','/students/'+student.id,null,token);
await request('GET','/students/'+other.id,null,token,403);
await request('POST','/courses',{title:'Denied',description:'Denied',credits:1},token,403);
for (const [service,port,route] of [['student',Number(process.env.SMOKE_STUDENT_PORT || 8081),'students'],['course',Number(process.env.SMOKE_COURSE_PORT || 8082),'courses'],['enrollment',Number(process.env.SMOKE_ENROLLMENT_PORT || 8083),'enrollments/student/'+cnie]]) {
  await request('GET','/'+route,null,null,401,'http://127.0.0.1:'+port+'/api');
}
console.log('PASS Gateway CRUD, login, ownership and direct-service security');

const enrollment = await request('POST','/enrollments',{studentCnie:cnie,courseId:course.id},token,201);
assert(enrollment.deletable);
assert(enrollment.enrollmentDate.endsWith('Z'));
await request('POST','/enrollments',{studentCnie:cnie,courseId:course.id},token,409);
await request('DELETE','/students/'+student.id,null,admin,409);
await request('DELETE','/courses/'+course.id,null,admin,409);
const listed = await request('GET','/enrollments/student/'+cnie,null,token);
assert.equal(listed.length,1);
await request('GET','/enrollments/student/'+other.cnie,null,token,403);
console.log('PASS discovered inter-service reads, enrollment, duplicates and deletion policy');

const frontend = process.env.SMOKE_FRONTEND_URL || 'http://127.0.0.1:4200';
const page = await fetch(frontend);
assert.equal(page.status,200);
assert((await page.text()).includes('<app-root>'));
await request('GET','/courses',null,token,200,frontend+'/api');
console.log('PASS Angular server and frontend-to-Gateway proxy');

await request('DELETE','/enrollments/'+enrollment.enrollmentId,null,token,204);
await request('DELETE','/enrollments/'+enrollment.enrollmentId,null,token,404);
await request('DELETE','/students/'+student.id,null,admin,204);
await request('DELETE','/students/'+other.id,null,admin,204);
await request('DELETE','/courses/'+course.id,null,admin,204);
console.log('PASS cancellation and eligible deletions; fixtures removed');
