import assert from 'node:assert/strict';
import test from 'node:test';
import { getPostLoginDestination } from '../src/pages/loginOrigin.js';

const baseOrigin = 'https://evidencepilot.test';

test('keeps valid private origins for the signed-in role', () => {
  assert.equal(
    getPostLoginDestination('/instructor/projects/project-1?tab=members', 'INSTRUCTOR', baseOrigin),
    '/instructor/projects/project-1?tab=members',
  );
  assert.equal(
    getPostLoginDestination('/student/projects/project-1', 'STUDENT', baseOrigin),
    '/student/projects/project-1',
  );
  assert.equal(
    getPostLoginDestination('/instructor/collections', 'ADMIN', baseOrigin),
    '/instructor/collections',
  );
});

test('falls back for public, foreign, unknown, or role-mismatched origins', () => {
  assert.equal(getPostLoginDestination('/', 'ADMIN', baseOrigin), '/admin/dashboard');
  assert.equal(getPostLoginDestination('/login', 'ADMIN', baseOrigin), '/admin/dashboard');
  assert.equal(getPostLoginDestination('/about', 'INSTRUCTOR', baseOrigin), '/instructor/dashboard');
  assert.equal(getPostLoginDestination('//other.example/path', 'INSTRUCTOR', baseOrigin), '/instructor/dashboard');
  assert.equal(getPostLoginDestination('/instructor/not-a-route', 'INSTRUCTOR', baseOrigin), '/instructor/dashboard');
  assert.equal(getPostLoginDestination('/admin/dashboard', 'INSTRUCTOR', baseOrigin), '/instructor/dashboard');
  assert.equal(getPostLoginDestination(null, 'STUDENT', baseOrigin), '/student/projects');
});
