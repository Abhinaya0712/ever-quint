import { render, screen, waitFor } from '@testing-library/react';
import App from './App';

beforeEach(() => localStorage.clear());

test('renders the workflow board and its three status columns', async () => {
  render(<App />);
  expect(
    screen.getByRole('heading', { name: /team workflow board/i })
  ).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Backlog' })).toBeInTheDocument();
  expect(
    screen.getByRole('heading', { name: 'In Progress' })
  ).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Done' })).toBeInTheDocument();
  await waitFor(() =>
    expect(localStorage.getItem('team-workflow-board')).not.toBeNull()
  );
});
