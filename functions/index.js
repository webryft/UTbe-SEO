// Deploy: firebase deploy --only functions   (needs Blaze plan)
const {onCall, HttpsError} = require('firebase-functions/v2/https');
const admin = require('firebase-admin');
admin.initializeApp();
const ADMIN = 'aladdincharlotte@gmail.com';

exports.adminUpdateUser = onCall(async (req) => {
  const t = req.auth && req.auth.token;
  if (!t || t.email !== ADMIN || !t.email_verified) throw new HttpsError('permission-denied', 'Admins only');
  const {uid, password, email} = req.data || {};
  if (!uid) throw new HttpsError('invalid-argument', 'uid required');
  const u = {};
  if (password) { if (password.length < 6) throw new HttpsError('invalid-argument', 'Password min 6 characters'); u.password = password; }
  if (email) u.email = email;
  await admin.auth().updateUser(uid, u);
  if (email) await admin.firestore().doc('users/' + uid).update({email});
  return {ok: true};
});
