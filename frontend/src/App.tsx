import Sidebar from "./components/Sidebar";
import AIChatWindow from "./components/AIChatWindow";
import ToDoElement from "./components/ToDoElement";
import { useState, useEffect } from "react";
import axios from "axios";

type TodoElement = {
	id: number;
	name: string;
	date: string;
	completed: boolean;
	belongsTo: string;
};

function App() {
	const [todoItems, setToDoItems] = useState<TodoElement[]>([]);
	const [todoLists, setTodoLists] = useState<Map<number, string>>(new Map());

	// Fetch ToDoLists from backend once on mount
	useEffect(() => {
		axios
			.get("http://localhost:8080/getToDoLists")
			.then((response) => {
				const lists = new Map<number, string>();
				Object.entries(response.data).forEach(([id, name]) => {
					lists.set(Number(id), name as string);
				});
				setTodoLists(lists);
			})
			.catch((error) => {
				console.error("Error fetching To-Do Lists:", error);
			});
	}, []);

	const handleAddTodoList = (newID: number, listName: string) => {
		setTodoLists(new Map(todoLists.set(newID, listName)));
	};

	const handleDeleteTodoList = (id: number) => {
		axios
			.delete("http://localhost:8080/deleteToDoList", { params: { id } })
			.then(() => {
				const newLists = new Map(todoLists);
				newLists.delete(id);
				setTodoLists(newLists);
				setToDoItems(todoItems.filter((item) => item.belongsTo !== id.toString()));
			})
			.catch((error) => {
				console.error("Error deleting To-Do Lists:", error);
			});
	};

	// Example logic for overdue (for demonstration)
	const today = new Date();
	const isOverdue = (date: string, completed: boolean) =>
		!completed && date && new Date(date).setHours(0, 0, 0, 0) < today.setHours(0, 0, 0, 0);

	const activeTasks = todoItems.filter(
		(todo) => !todo.completed && !isOverdue(todo.date, todo.completed)
	);
	const overdueTasks = todoItems.filter(
		(todo) => !todo.completed && isOverdue(todo.date, todo.completed)
	);
	const completedTasks = todoItems.filter((todo) => todo.completed);

	const handleDeleteCompleted = () => {
		setToDoItems(todoItems.filter((todo) => !todo.completed));
	};

	return (
		<>
			<Sidebar
				todoLists={todoLists}
				onAddTodoList={handleAddTodoList}
				onDeleteTodoList={handleDeleteTodoList}
			/>
			<div className='flex flex-col items-center justify-start min-h-screen bg-white dark:bg-gray-700 pt-8 pb-20'>
				{/* Active Section */}
				<section className='w-full flex flex-col items-center' style={{ height: "30vh" }}>
					<h2 className='text-2xl font-bold mb-2 text-black dark:text-white'>Active Tasks</h2>
					{activeTasks.length === 0 && <p className='text-gray-400'>No active tasks</p>}
					{activeTasks.map((todo) => (
						<ToDoElement key={todo.id} initialText={todo.name} initialDate={todo.date} />
					))}
				</section>
				{/* Overdue Section */}
				<section className='w-full flex flex-col items-center' style={{ height: "30vh" }}>
					<h2 className='text-2xl font-bold mb-2 text-red-600 dark:text-red-400'>Overdue Tasks</h2>
					{overdueTasks.length === 0 && <p className='text-gray-400'>No overdue tasks</p>}
					{overdueTasks.map((todo) => (
						<ToDoElement key={todo.id} initialText={todo.name} initialDate={todo.date} />
					))}
				</section>
				{/* Completed Section */}
				<section className='w-full flex flex-col items-center' style={{ height: "30vh" }}>
					<h2 className='text-2xl font-bold mb-2 text-green-600 dark:text-green-400'>
						Completed Tasks
					</h2>
					{completedTasks.length === 0 && <p className='text-gray-400'>No completed tasks</p>}
					{completedTasks.map((todo) => (
						<ToDoElement key={todo.id} initialText={todo.name} initialDate={todo.date} />
					))}
				</section>
				{/* Delete Button */}
				<div className='w-full flex justify-center mt-6 absolute bottom-8 left-0'>
					<button
						className='bg-red-500 hover:bg-red-700 text-white font-semibold py-2 px-6 rounded shadow'
						onClick={handleDeleteCompleted}
					>
						Delete All Completed Tasks
					</button>
				</div>
			</div>
			<AIChatWindow />
		</>
	);
}

export default App;
